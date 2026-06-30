package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "orders", description = "Order lifecycle management — create, list, status transitions, cancel")
public class OrderApiController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    @Operation(
        summary     = "Create an order",
        description = "Creates a new order in PENDING status. Inventory is deducted immediately. " +
                      "A WebSocket event is published to the kitchen display. " +
                      "Requires ADMIN, MANAGER, or WAITER role.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order payload with table, type, and items",
            required    = true,
            content     = @Content(schema = @Schema(implementation = CreateOrderRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created; Location header set",
                     content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",                content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",               content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role (CHEF)",        content = @Content),
        @ApiResponse(responseCode = "422", description = "Insufficient stock or table unavailable",
                     content = @Content)
    })
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long waiterId = request.getWaiterId() != null ? request.getWaiterId() : principal.getId();
        OrderResponse created = orderService.createOrder(request, waiterId);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.getId())).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER','CHEF')")
    @Operation(
        summary     = "List orders",
        description = "Returns a paginated, filterable list of orders sorted by creation time descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of orders returned",
                     content = @Content(schema = @Schema(implementation = PagedResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<PagedResponse<OrderResponse>> list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0")  int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Filter by order status", example = "PENDING")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Start of date range (ISO-8601)", example = "2025-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End of date range (ISO-8601)", example = "2025-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Filter by table ID", example = "5")
            @RequestParam(required = false) Long tableId,

            @Parameter(description = "Filter by waiter user ID", example = "12")
            @RequestParam(required = false) Long waiterId) {

        return ResponseEntity.ok(orderService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                status, from, to, tableId, waiterId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER','CHEF')")
    @Operation(summary = "Get order by ID", description = "Returns a single order with all items.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found",
                     content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",   content = @Content)
    })
    public ResponseEntity<OrderResponse> getById(
            @Parameter(description = "Order ID", example = "100") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@orderSecurityService.canUpdateStatus(#id, authentication)")
    @Operation(
        summary     = "Update order status",
        description = "Advances or cancels an order's status. Only valid state-machine transitions " +
                      "are accepted (e.g. PENDING→CONFIRMED, CONFIRMED→PREPARING). " +
                      "Cancelling after CONFIRMED restores deducted inventory. " +
                      "CHEF may only act on CONFIRMED/PREPARING orders.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New status and optional reason",
            required    = true,
            content     = @Content(schema = @Schema(implementation = OrderStatusUpdateRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated",
                     content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid state transition",  content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",         content = @Content),
        @ApiResponse(responseCode = "403", description = "Role not allowed for this transition",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",           content = @Content)
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "Order ID", example = "100") @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
        summary     = "Cancel an order",
        description = "Cancels the order. If already CONFIRMED or beyond, deducted inventory " +
                      "is restored. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order cancelled",    content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid transition", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",  content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role",  content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found",    content = @Content)
    })
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Order ID", example = "100") @PathVariable Long id,
            @Parameter(description = "Cancellation reason", example = "Customer changed their mind")
package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.AdjustStockRequest;
import com.restaurant.rms.dto.request.InventoryItemRequest;
import com.restaurant.rms.dto.response.InventoryItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.service.InventoryService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "inventory", description = "Inventory management — stock tracking, adjustments, low-stock alerts")
public class InventoryApiController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(
        summary     = "List inventory items (paginated)",
        description = "Returns a paginated slice of all inventory items. "
                    + "Use `?page=0&size=20&sort=name,asc` to control pagination and sorting. "
                    + "Requires ADMIN or MANAGER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role",  content = @Content)
    })
    public ResponseEntity<PagedResponse<InventoryItemResponse>> list(
            @Parameter(description = "Zero-based page index",  example = "0")  @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size (max 100)",    example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction, e.g. name,asc", example = "name,asc")
                @RequestParam(defaultValue = "name,asc") String sort) {

        // Parse "field,direction" — fall back to "name ASC" on malformed input.
        String[] parts     = sort.split(",", 2);
        String   sortField = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Clamp page size to prevent denial-of-service via huge pages.
        int clampedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(page, clampedSize, Sort.by(dir, sortField));
        return ResponseEntity.ok(inventoryService.findAll(pageable));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock items", description = "Returns items whose current stock is at or below their minimum threshold.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Low-stock items returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<List<InventoryItemResponse>> lowStock() {
        return ResponseEntity.ok(inventoryService.findLowStockItems());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory item by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item found"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<InventoryItemResponse> getById(
            @Parameter(description = "Inventory item ID", example = "7") @PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.findById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create inventory item",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Inventory item details", required = true,
            content = @Content(schema = @Schema(implementation = InventoryItemRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item created"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name", content = @Content)
    })
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse created = inventoryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory item")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<InventoryItemResponse> update(
            @Parameter(description = "Inventory item ID", example = "7") @PathVariable Long id,
            @Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.ok(inventoryService.update(id, request));
    }

    @PostMapping("/{id}/adjust")
    @Operation(
        summary     = "Adjust stock level",
        description = "Adds or subtracts stock. Use a negative quantity to deduct. " +
                      "Throws 400 if resulting stock would go below zero.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Quantity delta and reason", required = true,
            content = @Content(schema = @Schema(implementation = AdjustStockRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjusted"),
        @ApiResponse(responseCode = "400", description = "Would result in negative stock", content = @Content),
        @ApiResponse(responseCode = "404", description = "Item not found", content = @Content)
    })
    public ResponseEntity<InventoryItemResponse> adjustStock(
            @Parameter(description = "Inventory item ID", example = "7") @PathVariable Long id,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(id, request));
    }
}

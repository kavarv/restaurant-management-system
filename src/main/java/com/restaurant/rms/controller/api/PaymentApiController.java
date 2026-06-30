package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.PaymentRequest;
import com.restaurant.rms.dto.response.PaymentResponse;
import com.restaurant.rms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "payments", description = "Payment processing — charge, refund, receipt retrieval")
public class PaymentApiController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    @Operation(
        summary = "Process a payment",
        description = "Records and processes payment for a completed order. " +
                      "Updates the order status to COMPLETED upon success.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Payment details", required = true,
            content = @Content(schema = @Schema(implementation = PaymentRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment processed",
                     content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or order already paid", content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse created = paymentService.processPayment(request);
        return ResponseEntity.created(URI.create("/api/v1/payments/" + created.getId())).body(created);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    @Operation(summary = "Get payment by order ID", description = "Returns the payment record for the given order.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "No payment for this order", content = @Content)
    })
    public ResponseEntity<PaymentResponse> getByOrder(
            @Parameter(description = "Order ID", example = "100") @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.findByOrder(orderId));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Refund a payment", description = "Issues a full refund. Requires ADMIN or MANAGER.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund processed"),
        @ApiResponse(responseCode = "400", description = "Payment not refundable", content = @Content),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ResponseEntity<PaymentResponse> refund(
            @Parameter(description = "Payment ID", example = "55") @PathVariable Long paymentId) {
    
package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for POST /api/payments — initiates or records a payment for an order.
 */
@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be a positive number")
    private Long orderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * Optional gateway transaction reference (e.g. Razorpay payment_id).
     * Null for cash payments; the service generates / stores this for card/UPI.
     */
    private String transactionId;
}

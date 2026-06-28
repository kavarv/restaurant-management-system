package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.PaymentRequest;
import com.restaurant.rms.dto.response.PaymentResponse;

/**
 * Handles payment processing and refunds for completed orders.
 */
public interface PaymentService {

    /**
     * Processes a payment for an order.
     * <ul>
     *   <li>Validates order is in SERVED or COMPLETED status</li>
     *   <li>Checks no duplicate payment exists for the order</li>
     *   <li>Records the payment and marks the order as COMPLETED</li>
     * </ul>
     *
     * @param request payment details (orderId, amount, method, transactionId)
     * @return created payment DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if order not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if order is not in a payable state
     *         or a payment already exists for this order
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Returns the payment record for a given order.
     *
     * @param orderId order ID
     * @return payment DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if no payment found for the order
     */
    PaymentResponse findByOrder(Long orderId);

    /**
     * Issues a refund for a completed payment.
     *
     * @param paymentId payment ID to refund
     * @return updated payment DTO with status REFUNDED
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if payment not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if payment is not in COMPLETED status
     */
    PaymentResponse refund(Long paymentId);
}

package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.PaymentRequest;
import com.restaurant.rms.dto.response.PaymentResponse;
import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.Payment;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.PaymentStatus;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.repository.PaymentRepository;
import com.restaurant.rms.service.AuditService;
import com.restaurant.rms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (order.getStatus() != OrderStatus.SERVED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new InvalidOperationException("processPayment",
                    "order must be in SERVED or COMPLETED status, current: " + order.getStatus());
        }

        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new InvalidOperationException("processPayment",
                    "a payment already exists for order " + order.getId());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .transactionId(request.getTransactionId())
                .paidAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        // Mark order as COMPLETED after payment
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        auditService.log("Payment", saved.getId(), AuditAction.CREATE,
                null, "orderId=" + order.getId() + " amount=" + request.getAmount());
        log.info("Payment processed id={} orderId={} method={}", saved.getId(),
                order.getId(), request.getPaymentMethod());
        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrder(Long orderId) {
        return PaymentResponse.from(
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId)));
    }

    @Override
    @Transactional
    public PaymentResponse refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidOperationException("refund",
                    "only COMPLETED payments can be refunded, current: " + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);
        auditService.log("Payment", paymentId, AuditAction.UPDATE,
                PaymentStatus.COMPLETED.name(), PaymentStatus.REFUNDED.name());
        log.info(
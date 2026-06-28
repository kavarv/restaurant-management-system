package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.PaymentMethod;
import com.restaurant.rms.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records the payment transaction for a completed {@link Order}.
 *
 * <p>The relationship is a strict 1-to-1: one Order has at most one Payment record.
 * If a payment fails and is retried, the same Payment row is updated rather than
 * creating a new one.</p>
 *
 * <p>Fetch-type decision:</p>
 * <ul>
 *   <li>{@code order} — LAZY: Payment is always loaded through Order.payment;
 *       re-fetching Order eagerly here would create a bidirectional cycle.</li>
 * </ul>
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(name = "uk_payments_order", columnNames = "order_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "order")
public class Payment extends BaseEntity {

    /**
     * LAZY: Payment is always navigated from Order (Order → Payment); the
     * reverse eager navigation from Payment → Order would risk loading the
     * entire order graph unnecessarily.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Gateway transaction ID — null for cash payments. */
    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    /** Timestamp recorded by the payment gateway (may differ from createdAt). */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;
}

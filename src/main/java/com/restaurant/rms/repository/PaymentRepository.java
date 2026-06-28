package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Payment;
import com.restaurant.rms.entity.enums.PaymentMethod;
import com.restaurant.rms.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByPaymentMethod(PaymentMethod method);

    /** Reconciliation report by tender type. Returns Object[]{method, total, count}. */
    @Query("""
            SELECT p.paymentMethod, SUM(p.amount), COUNT(p)
            FROM Payment p
            WHERE p.status = com.restaurant.rms.entity.enums.PaymentStatus.COMPLETED
              AND p.createdAt BETWEEN :start AND :end
            GROUP BY p.paymentMethod
            """)
    List<Object[]> getRevenueByPaymentMethod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Failed-payment queue — for manual investigation or retry. */
    List<Payment> findByStatusAndCreatedAtBetween(
            PaymentStatus status, LocalDateTime start, LocalDateTime end);
}

package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A table booking — created either by staff (with a linked User account)
 * or by a public visitor (stored as plain contact strings, no account needed).
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"customer", "table"})
public class Reservation extends BaseEntity {

    /**
     * Linked user account — set for staff-created reservations or when a
     * logged-in customer books. NULL for public (anonymous) reservations.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "customer_id", nullable = true,
                foreignKey = @ForeignKey(name = "fk_reservations_customer"))
    private User customer;

    /** Contact name for public (anonymous) reservations. */
    @Column(name = "customer_name", length = 150)
    private String customerName;

    /** Contact email for public reservations. */
    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    /** Contact phone for public reservations. */
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "table_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reservations_table"))
    private RestaurantTable table;

    @Column(name = "reserved_date", nullable = false)
    private LocalDateTime reservedDate;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmation_code", length = 20)
    private String confirmationCode;

    @PrePersist
    private void generateConfirmationCode() {
        if (confirmationCode == null || confirmationCode.isBlank()) {
            confirmationCode = "RES-" + System.currentTimeMillis() % 1_000_000;
        }
    }
}

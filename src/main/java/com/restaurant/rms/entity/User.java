package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents every human actor in the system — customers, staff, admins.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code ordersHandled} — LAZY: a User may handle hundreds of orders over time;
 *       loading them eagerly would be a severe N+1 risk for any query that loads users.</li>
 *   <li>{@code reservations} — LAZY: same reasoning; reservations are only needed in
 *       dedicated reservation-management flows.</li>
 * </ul>
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email",    columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ordersHandled", "reservations"})
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /** Bcrypt-hashed password — never stored as plain text. */
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ------------------------------------------------------------------ //
    //  Bidirectional relationships — mapped on the owning (FK) side       //
    // ------------------------------------------------------------------ //

    /**
     * Orders assigned to this user as waiter/chef. LAZY — not needed when
     * loading a user for authentication or profile display.
     */
    @OneToMany(mappedBy = "waiter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<Order> ordersHandled = new java.util.ArrayList<>();

    /**
     * Reservations created by this user (customer role). LAZY — reservation
     * history is loaded only in the reservation management context.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<Reservation> reservations = new java.util.ArrayList<>();
}

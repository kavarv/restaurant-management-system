package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * An Order groups one or more {@link OrderItem}s placed by a guest
 * at a specific table (or for takeaway/delivery).
 *
 * <p>Fetch-type decisions:</p>
 * <ul>
 *   <li>{@code table} — EAGER: the table number is shown on every order summary
 *       screen; fetching it lazily would add a secondary query on every order load.</li>
 *   <li>{@code waiter} — EAGER: waiter name is part of the order header displayed
 *       in the kitchen and billing views.</li>
 *   <li>{@code items} — LAZY: the item list is large and needed only when rendering
 *       the full order detail or the kitchen ticket, not on list views.</li>
 *   <li>{@code payment} — LAZY: payment detail is only needed in the billing flow.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"items", "payment"})
public class Order extends BaseEntity {

    /**
     * EAGER: table number is part of every order header — one extra join
     * is cheaper than N lazy loads on a list screen.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "table_id",
                foreignKey = @ForeignKey(name = "fk_orders_table"))
    private RestaurantTable table;

    /**
     * EAGER: waiter name is displayed alongside every order in the kitchen
     * display and the management dashboard.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "waiter_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_orders_waiter"))
    private User waiter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Running total, recalculated in the service layer on each item add/remove. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Populated for DELIVERY orders. */
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    /** LAZY: item list is only needed when rendering the full ticket. */
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** LAZY: payment detail only required in the billing/checkout flow. */
    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY,
              cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    // ------------------------------------------------------------------ //
    //  Lifecycle hooks                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Ensures total amount is never null at INSERT time (defensive; service
     * should set this properly before persisting).
     */
    @PrePersist
    private void prePersist() {
 
package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.OrderItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single line in an {@link Order}, referencing one {@link MenuItem}.
 *
 * <p>Fetch-type decisions:</p>
 * <ul>
 *   <li>{@code order} — LAZY: always loaded through the parent Order's item collection;
 *       back-navigating eagerly would create a circular load cycle.</li>
 *   <li>{@code menuItem} — EAGER: item name and price are needed every time an
 *       OrderItem is rendered, whether on the kitchen ticket or the bill.</li>
 * </ul>
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "menuItem"})
public class OrderItem extends BaseEntity {

    /**
     * LAZY: OrderItem is always accessed through Order.items; loading Order back
     * from here eagerly would cause a bidirectional eager-loading cycle.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    /**
     * EAGER: name and price are displayed on every kitchen ticket and bill line —
     * one join at load time avoids N secondary queries.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_items_menu_item"))
    private MenuItem menuItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price snapshotted at the time of order — ensures historical accuracy
     * even if the menu item price is later changed.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.PENDING;

    // ------------------------------------------------------------------ //
    //  Computed convenience                                                //
    // ------------------------------------------------------------------ //

    /** @return {@code unitPrice × quantity} as a line total. */
    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ------------------------------------------------------------------ //
    //  Lifecycle hooks                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Snapshot the current menu item price on INSERT so historical orders
     * remain accurate after future price changes.
     */
    @PrePersist
    private void snapshotPrice() {
        if (unitPrice == null && menuItem != null) {
            unitPrice = menuItem.getPrice();
        }
    }
}

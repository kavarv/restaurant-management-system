package com.restaurant.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Join table between {@link MenuItem} and {@link InventoryItem} that also carries
 * the quantity of each ingredient required per one unit of the dish.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code menuItem} — LAZY: when loading ingredient details we already have the
 *       menu item in context; re-fetching it eagerly would be redundant.</li>
 *   <li>{@code inventoryItem} — EAGER: the ingredient name and unit are always needed
 *       when rendering ingredient lists, so eager loading avoids N+1 per row.</li>
 * </ul>
 */
@Entity
@Table(
    name = "menu_item_ingredients",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_menu_ingredient",
        columnNames = {"menu_item_id", "inventory_item_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"menuItem", "inventoryItem"})
public class MenuItemIngredient extends BaseEntity {

    /**
     * LAZY: we always load MenuItemIngredient through its parent MenuItem collection;
     * back-navigating to MenuItem from here would create a circular eager load.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_mii_menu_item"))
    private MenuItem menuItem;

    /**
     * EAGER: ingredient name and unit are always rendered next to the quantity —
     * this single join is far cheaper than N lazy loads for each ingredient row.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_mii_inventory_item"))
    private InventoryItem inventoryItem;

    /** Quantity of the inventory item required per one serving of the dish. */
    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    /**
     * Override unit for this specific use — e.g. the inventory item is tracked
     * in "kg" but the recipe calls for "g".  Nullable; falls back to InventoryItem.unit.
     */
    @Column(name = "uni
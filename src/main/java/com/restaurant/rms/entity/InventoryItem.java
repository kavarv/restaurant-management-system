package com.restaurant.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A raw ingredient or supply tracked in inventory (e.g. "Tomato", "Olive Oil").
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code menuItemIngredients} — LAZY: the link table data (which dishes use this
 *       ingredient) is only needed in inventory/recipe management screens.</li>
 * </ul>
 */
@Entity
@Table(
    name = "inventory_items",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "menuItemIngredients")
public class InventoryItem extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Unit of measurement, e.g. "kg", "litre", "piece". */
    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "current_stock", nullable = false, precision = 10, scale = 3)
    private BigDecimal currentStock;

    /** Alert threshold — a stock level below this triggers a low-stock warning. */
    @Column(name = "minimum_stock", nullable = false, precision = 10, scale = 3)
    private BigDecimal minimumStock;

    @Column(name = "cost_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPerUnit;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    /** LAZY: which dishes use this item is only needed in recipe management. */
    @OneToMany(mappedBy = "inventoryItem", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItemI
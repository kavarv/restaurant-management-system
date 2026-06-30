package com.restaurant.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A dish or beverage available for order. Extends {@link SoftDeletableEntity}
 * so that removing an item from the menu does not orphan historical order records.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code category} — EAGER: virtually every read of a menu item includes its
 *       category name for display; the extra join is negligible and avoids N+1 on
 *       menu-listing screens.</li>
 *   <li>{@code ingredients} — LAZY: ingredient detail is only needed in the
 *       kitchen or inventory management context, not for normal menu display.</li>
 *   <li>{@code orderItems} — LAZY: historical order data is never needed when
 *       browsing the menu.</li>
 * </ul>
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ingredients", "orderItems"})
public class MenuItem extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Stored as DECIMAL(10,2) in MySQL.  Using {@link BigDecimal} avoids
     * floating-point rounding errors on monetary values.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * EAGER: category name is displayed alongside every menu item —
     * eager loading eliminates a secondary query on every item render.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_menu_items_category"))
    private Category category;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_vegetarian", nullable = false)
    @Builder.Default
    private Boolean isVegetarian = false;

    @Column(name = "is_vegan", nullable = false)
    @Builder.Default
    private Boolean isVegan = false;

    @Column(name = "is_gluten_free", nullable = false)
    @Builder.Default
    private Boolean isGlutenFree = false;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Estimated preparation time in minutes. */
    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Column(name = "calories")
    private Integer calories;

    /** LAZY: ingredient breakdown is only needed in kitchen/inventory context. */
    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItemIngredient> ingredients = new ArrayList<>();

    /** LAZY: order history is never needed when displaying the menu. */
    @OneToMany(mappedBy = "menuItem"
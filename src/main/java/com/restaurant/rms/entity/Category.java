package com.restaurant.rms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Menu category (e.g. Starters, Mains, Desserts, Beverages).
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code menuItems} — LAZY: a category may contain many items; we almost never
 *       need all of them when fetching a category for breadcrumb or header display.</li>
 * </ul>
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "menuItems")
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** LAZY: menu items are loaded separately via MenuItemRepository. */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<MenuItem> m
/**
 * JPA entity classes mapped to MySQL tables.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Each entity is annotated with {@code @Entity} and {@code @Table(name = "...")} </li>
 *   <li>Primary keys use {@code @GeneratedValue(strategy = GenerationType.IDENTITY)}</li>
 *   <li>Audit fields (createdAt, updatedAt) use {@code @CreationTimestamp} /
 *       {@code @UpdateTimestamp} or Spring Data's {@code @EntityListeners(AuditingEntityListener.class)}</li>
 *   <li>No business logic in entities — keep them as pure data models</li>
 * </ul>
 *
 * <p>Planned entities: User, Role, MenuItem, MenuCategory, RestaurantTable,
 * Order, OrderItem, Payment, Reservation.
 */
package com.restaurant.rms.entity;

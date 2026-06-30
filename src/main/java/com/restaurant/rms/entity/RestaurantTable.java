package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * A physical dining table in the restaurant.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@code orders} — LAZY: a table accumulates many orders over its lifetime;
 *       fetching them eagerly on every table lookup is wasteful.</li>
 *   <li>{@code reservations} — LAZY: same reasoning as orders.</li>
 * </ul>
 */
@Entity
@Table(
    name = "restaurant_tables",
    uniqueConstraints = @UniqueConstraint(name = "uk_tables_number", columnNames = "table_number")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"orders", "reservations"})
public class RestaurantTable extends BaseEntity {

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(name = "location_description", length = 100)
    private String locationDescription;

    // ------------------------------------------------------------------ //
    //  Bidirectional relationships                                         //
    // ------------------------------------------------------------------ //

    /** LAZY: table-level queries (seating, status) rarely need order history. */
    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<Order> orders = new java.util.ArrayList<>();

    /** LAZY: reservation lists are loaded only in the reservation context. */
    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<Reserv
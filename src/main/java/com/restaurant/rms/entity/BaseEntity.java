package com.restaurant.rms.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Abstract base entity providing primary key and audit timestamps.
 *
 * <p>@EqualsAndHashCode(of = "id") ensures that two entities are considered equal
 * iff they share the same database PK, which is the correct semantic for JPA entities.
 * Using the default Lombok equals (all fields) would break collections when mutable
 * state changes after an object is already stored in a HashSet/HashMap.</p>
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Set automatically by Hibernate on INSERT; never updated afterward.
     * Mapped to a DATETIME(6) column so microseconds are preserved.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated by Hibernate on every UPDATE.  Null until the first flush after
     * the initial INSERT (which Hibernate then immediately corrects to the INSERT time).
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

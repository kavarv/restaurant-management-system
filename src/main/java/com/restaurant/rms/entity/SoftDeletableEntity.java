package com.restaurant.rms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Extends {@link BaseEntity} with soft-delete semantics.
 *
 * <h3>How {@code @SQLRestriction} works in Hibernate 6 (Spring Boot 3.x)</h3>
 * <p>{@code @SQLRestriction("deleted_at IS NULL")} (the Hibernate 6 replacement for the
 * deprecated {@code @Where}) appends a literal SQL predicate to <em>every</em> query
 * Hibernate generates for this entity — SELECT, JOIN fetches, and collection loads
 * alike.  The predicate is injected at the SQL level (not JPQL), so it is evaluated by
 * the database engine rather than in memory, making it both safe and efficient.</p>
 *
 * <p>Consequences to be aware of:</p>
 * <ul>
 *   <li>A soft-deleted row is <em>completely invisible</em> to normal JPA/JPQL queries;
 *       you must use a native query or {@code EntityManager.createNativeQuery} to see
 *       deleted records.</li>
 *   <li>The restriction also applies when Hibernate resolves a {@code @ManyToOne} by PK
 *       (a proxy lookup) — if the target row is soft-deleted the reference returns
 *       {@code null}.</li>
 *   <li>To "hard-delete" a soft-deleted row, bypass JPA entirely and issue a native
 *       {@code DELETE} statement.</li>
 * </ul>
 *
 * <p>Soft-delete pattern: set {@code deletedAt = LocalDateTime.now()} and call
 * {@code entityManager.merge()} (or Spring Data's {@code save()}).  The row remains in
 * the database but disappears from all standard queries.</p>
 */
@MappedSuperclass
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Convenience predicate — avoids null-check boilerplate in business code.
     *
     * @return {@code true} if this entity has been soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}

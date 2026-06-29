package com.restaurant.rms.entity;

import com.restaurant.rms.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry.  One row is written for every CREATE / UPDATE /
 * DELETE / RESTORE operation on a tracked entity.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>AuditLog does <em>not</em> extend BaseEntity; the PK is still a IDENTITY
 *       Long, but we skip the updateTimestamp — audit records are never modified.</li>
 *   <li>{@code changedBy} is EAGER: the author name is always shown next to an audit
 *       entry; loading it lazily would produce N+1 on any audit trail page.</li>
 *   <li>{@code oldValues} and {@code newValues} are stored as JSON strings so that
 *       any entity structure can be audited without schema changes.</li>
 * </ul>
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_changed_by", columnList = "changed_by_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "changedBy")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Simple class name of the entity being audited, e.g. "MenuItem". */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /** PK of the audited row. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    /**
     * EAGER: author name is always displayed on every audit row —
     * eagerly joining the users table is cheaper than N lazy queries.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_id",
                foreignKey = @ForeignKey(name = "fk_audit_changed_by"))
    private User changedBy;

    /** JSON snapshot of the row before the mutation (null for CREATE). */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    /** JSON snapshot of the row after the mutation (null for DELETE). */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    private void prePersist() {
        changedAt = LocalDateTime.now();
    }
}

package com.restaurant.rms.repository;

import com.restaurant.rms.entity.AuditLog;
import com.restaurant.rms.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            String entityType, Long entityId);

    Page<AuditLog> findByEntityTypeAndEntityId(
            String entityType, Long entityId, Pageable pageable);

    List<AuditLog> findByChangedByIdOrderByChangedAtDesc(Long changedById);

    Page<AuditLog> findByChangedById(Long changedById, Pageable pageable);

    List<AuditLog> findByActionAndChangedAtBetween(
            AuditAction action, LocalDateTime start, LocalDateTime end);

    Page<AuditLog> findByChangedAtBetweenOrderByChangedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Filtered, paginated audit log for the admin console.
     * All params are optional — null values are ignored.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:userId      IS NULL OR a.changedBy.id = :userId)
              AND (:from        IS NULL OR a.changedAt >= :from)
              AND (:to          IS NULL OR a.changedAt <= :to)
            ORDER BY a.changedAt DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("entityType") String entityType,
            @Param("userId")     Long userId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    @Query("""
            SELECT a.entityType, a.action, COUNT(a)
            FROM AuditLog a
            WHERE a.changedAt BETWEEN :start AND :end
            GROUP BY a.entityType, a.action
            ORDER BY a.entityType, a.action
            """)
    List<Object[]> getActivitySummary(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end);
}

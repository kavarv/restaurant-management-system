package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.AuditLog;
import com.restaurant.rms.entity.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Flattened view of an {@link AuditLog} row for admin UI and API responses.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLogResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private AuditAction action;

    private Long changedById;
    private String changedByUsername;

    private String oldValues;
    private String newValues;

    private String ipAddress;
    private LocalDateTime changedAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .changedById(log.getChangedBy() != null ? log.getChangedBy().getId() : null)
                .changedByUsername(log.getChangedBy() != null ? log.getChangedBy().getUsername() : "system")
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .ipAddress(log.getIpAddress())
                .changedAt(log.getChangedA
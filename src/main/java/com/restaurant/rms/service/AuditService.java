package com.restaurant.rms.service;

import com.restaurant.rms.dto.response.AuditLogResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

/**
 * Records immutable audit events and exposes query methods for the admin UI.
 *
 * <h3>Failure isolation contract</h3>
 * <p>Implementations MUST catch all exceptions internally and log them rather
 * than re-throwing, so a logging failure never rolls back the calling business
 * transaction.</p>
 */
public interface AuditService {

    /**
     * Records an audit event with full HTTP context (IP, User-Agent) and
     * the current authenticated user from the Security context.
     *
     * @param entityType simple class name of the audited entity, e.g. "MenuItem"
     * @param entityId   PK of the audited row
     * @param action     CREATE | UPDATE | DELETE | RESTORE
     * @param oldValues  object to serialize as JSON "before" snapshot (null for CREATE)
     * @param newValues  object to serialize as JSON "after"  snapshot (null for DELETE)
     * @param request    the current HTTP request — used to extract IP and User-Agent;
     *                   pass {@code null} when called outside an HTTP context (e.g. batch)
     */
    void log(String entityType, Long entityId, AuditAction action,
             Object oldValues, Object newValues, HttpServletRequest request);

    /**
     * Convenience overload for legacy callers that don't have the HTTP request
     * (passes {@code null} for request — IP/UA will be blank in the audit record).
     */
    void log(String entityType, Long entityId, AuditAction action,
             String oldValues, String newValues);

    PagedResponse<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable);

    PagedResponse<AuditLogResponse> findByUser(Long userId, Pageable pageable);

    PagedResponse
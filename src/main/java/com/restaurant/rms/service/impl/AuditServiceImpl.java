package com.restaurant.rms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.dto.response.AuditLogResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.AuditLog;
import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.repository.AuditLogRepository;
import com.restaurant.rms.repository.UserRepository;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes immutable audit log entries to the database.
 *
 * <h3>Failure isolation</h3>
 * <p>All public methods wrap their logic in a broad {@code try/catch} and log
 * failures rather than re-throwing. This guarantees that audit logging never
 * rolls back or interrupts the caller's business transaction.</p>
 *
 * <h3>Transaction propagation</h3>
 * <p>{@code REQUIRES_NEW} on the write path creates a separate mini-transaction
 * for the audit insert. If the caller's transaction later rolls back (e.g. an
 * OptimisticLockException), the audit record is still committed — providing
 * a complete and honest trail of attempted mutations.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository     userRepository;
    private final ObjectMapper       objectMapper;

    // ──────────────────────────────────────────────────────────────────────────
    //  Write — full HTTP context overload
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serializes {@code oldValues} and {@code newValues} to JSON using Jackson,
     * extracts the client IP from the request (honouring X-Forwarded-For for
     * reverse-proxied deployments), and persists the audit record in its own
     * transaction so a rollback on the caller side does not erase the trail.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, Long entityId, AuditAction action,
                    Object oldValues, Object newValues, HttpServletRequest request) {
        try {
            String oldJson = toJson(oldValues);
            String newJson = toJson(newValues);
            String ip      = extractIp(request);

            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldValues(oldJson)
                    .newValues(newJson)
                    .ipAddress(ip)
                    .changedBy(resolveCurrentUser())
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit [{}] {} id={} by={} ip={}",
                    action, entityType, entityId,
                    entry.getChangedBy() != null ? entry.getChangedBy().getUsername() : "system",
                    ip);
        } catch (Exception ex) {
            // Never propagate — log and continue
            log.error("Audit log write failed [type={}, id={}, action={}]: {}",
                    entityType, entityId, action, ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Write — legacy string overload (no HTTP context)
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, Long entityId, AuditAction action,
                    String oldValues, String newValues) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .changedBy(resolveCurrentUser())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Audit log write failed [type={}, id={}, action={}]: {}",
                    entityType, entityId, action, ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Query methods
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> findByEntity(String entityType, Long entityId,
                                                         Pageable pageable) {
        Page<AuditLog> page = auditLogRepository
                .findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return PagedResponse.from(page, AuditLogResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> findByUser(Long userId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByChangedById(userId, pageable);
        return PagedResponse.from(page, AuditLogResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> findRecent(int limit) {
        Pageable p = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "changedAt"));
        Page<AuditLog> page = auditLogRepository.findAll(p);
        return PagedResponse.from(page, AuditLogResponse::from);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serializes any object to a compact JSON string.
     * Strings are returned as-is (assumed to be pre-serialized or a label).
     * Null returns null (stored as SQL NULL).
     */
    private String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("AuditService: failed to serialize value to JSON: {}", ex.getMessage());
            return value.toString();
        }
    }

    /**
     * Extracts the real client IP address from the request.
     *
     * <p>When the app is behind a reverse proxy (nginx, AWS ALB, Cloudflare),
     * the actual client IP arrives in {@code X-Forwarded-For}. The header may
     * be a comma-separated list; only the first entry is the original client.
     * We fall back to {@code REMOTE_ADDR} when no proxy header is present.</p>
     */
    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "client, proxy1, proxy2" — take the leftmost (original client)
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    /**
     * Resolves the currently authenticated {@link User} from the Spring Security
     * context. Returns {@code null} for unauthenticated / system-triggered calls
     * so the audit record is still persisted with a null {@code changedBy}.
     */
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return userRepository.findById(up.getId()
package com.restaurant.rms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.dto.response.AuditLogResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.AuditLog;
import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.entity.enums.Role;
import com.restaurant.rms.repository.AuditLogRepository;
import com.restaurant.rms.repository.UserRepository;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.impl.AuditServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditServiceImpl}.
 *
 * <p>Covers:
 * <ul>
 *   <li>JSON serialization of old/new values via Jackson</li>
 *   <li>IP extraction from X-Forwarded-For and REMOTE_ADDR</li>
 *   <li>Current-user resolution from the Security context</li>
 *   <li>Failure isolation — exceptions must never propagate to callers</li>
 *   <li>REQUIRES_NEW transaction propagation is declared (structural test)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository     userRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    // Real ObjectMapper — we want to verify actual JSON output
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectObjectMapper() throws Exception {
        // Inject the real ObjectMapper via reflection since @InjectMocks uses the mock
        var field = AuditServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(auditService, objectMapper);
    }

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── JSON serialization ────────────────────────────────────────────────────

    @Test
    void log_serializesMapValuesToJson() {
        Map<String, Object> oldState = Map.of("name", "Burger", "price", "10.00");
        Map<String, Object> newState = Map.of("name", "Burger Deluxe", "price", "12.50");

        HttpServletRequest req = mockRequest("127.0.0.1", null);
        auditService.log("MenuItem", 1L, AuditAction.UPDATE, oldState, newState, req);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getOldValues()).contains("Burger");
        assertThat(saved.getNewValues()).contains("Burger Deluxe");
        assertThat(saved.getNewValues()).contains("12.50");
        assertThat(saved.getEntityType()).isEqualTo("MenuItem");
        assertThat(saved.getEntityId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.UPDATE);
    }

    @Test
    void log_persistsNullOldValuesForCreateAction() {
        HttpServletRequest req = mockRequest("10.0.0.1", null);
        auditService.log("MenuItem", 5L, AuditAction.CREATE,
                (Object) null, Map.of("name", "New Dish"), req);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getOldValues()).isNull();
        assertThat(captor.getValue().getNewValues()).isNotBlank();
    }

    // ── IP extraction ─────────────────────────────────────────────────────────

    @Test
    void log_extractsIpFromRemoteAddr_whenNoForwardedFor() {
        HttpServletRequest req = mockRequest("192.168.1.50", null);
        auditService.log("MenuItem", 1L, AuditAction.CREATE, null, "created", req);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.50");
    }

    @Test
    void log_prefersXForwardedForOverRemoteAddr() {
        HttpServletRequest req = mockRequest("10.0.0.1", "203.0.113.5, 10.0.0.1");
        auditService.log("MenuItem", 1L, AuditAction.UPDATE, "old", "new", req);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        // Should use the leftmost (original client) IP from X-Forwarded-For
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    void log_handlesNullRequest_gracefully() {
        assertThatNoException().isThrownBy(() ->
                auditService.log("Order", 99L, AuditAction.DELETE, "old", null, null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    // ── Current-user resolution ───────────────────────────────────────────────

    @Test
    void log_setsChangedByFromSecurityContext() {
        User user = buildUser(7L, "bob");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        setSecurityContext(user);

        auditService.log("InventoryItem", 3L, AuditAction.UPDATE, "old", "new", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNotNull();
        assertThat(captor.getValue().getChangedBy().getUsername()).isEqualTo("bob");
    }

    @Test
    void log_setsChangedByToNull_whenNotAuthenticated() {
        // No security context set — anonymous request
        auditService.log("Order", 1L, AuditAction.CREATE, null, "new", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNull();
    }

    // ── Failure isolation ─────────────────────────────────────────────────────

    @Test
    void log_doesNotThrow_whenRepositorySaveFails() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        // Must not propagate — the caller's transaction must be unaffected
        assertThatNoException().isThrownBy(() ->
                auditService.log("MenuItem", 1L, AuditAction.UPDATE, "old", "new", null));
    }

    @Test
    void log_doesNotThrow_whenJacksonSerializationFails() throws Exception {
        // Create an object that Jackson cannot serialize
        Object unserializable = new Object() {
            public Object getSelf() { return this; } // circular reference
        };

        assertThatNoException().isThrownBy(() ->
                auditService.log("MenuItem", 1L, AuditAction.UPDATE,
                        unserializable, "new value", null));
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    @Test
    void findByEntity_returnsPagedResponse() {
        AuditLog log = buildAuditLog(1L, "MenuItem", 42L, AuditAction.CREATE);
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByEntityTypeAndEntityId(
                eq("MenuItem"), eq(42L), any(Pageable.class))).thenReturn(page);

        PagedResponse<AuditLogResponse> result =
                auditService.findByEntity("MenuItem", 42L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEntityType()).isEqualTo("MenuItem");
        assertThat(result.getContent().get(0).getEntityId()).isEqualTo(42L);
    }

    @Test
    void findByUser_returnsPagedResponse() {
        AuditLog log = buildAuditLog(2L, "Order", 10L, AuditAction.UPDATE);
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByChangedById(eq(5L), any(Pageable.class))).thenReturn(page);

        PagedResponse<AuditLogResponse> result =
                auditService.findByUser(5L, PageRequest.of(0, 25));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpServletRequest mockRequest(String remoteAddr, String xForwardedFor) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn(remoteAddr);
        lenient().when(req.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        lenient().when(req.getHeader("X-Real-IP")).thenReturn(null);
        return req;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        user.setIsActive(true);
        return user;
    }

    private void setSecurityContext(User user) {
        UserPrincipal principal = UserPrincipal.fromUser(user);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AuditLog buildAuditLog(Long id, String entityType, Long entityId, AuditAction action) {
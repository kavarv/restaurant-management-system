package com.restaurant.rms.security;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderSecurityService}.
 *
 * <p>Verifies the SpEL-callable method {@code canUpdateStatus(orderId, auth)}
 * for all role/state combinations defined in the business rules.</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderSecurityServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderSecurityService service;

    // ── CHEF rules ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("CHEF can update CONFIRMED order (CONFIRMED → PREPARING)")
    void chef_canUpdate_confirmed() {
        Authentication auth = mockAuth("ROLE_CHEF");
        stubOrder(1L, OrderStatus.CONFIRMED);

        assertThat(service.canUpdateStatus(1L, auth)).isTrue();
    }

    @Test
    @DisplayName("CHEF can update PREPARING order (PREPARING → READY)")
    void chef_canUpdate_preparing() {
        Authentication auth = mockAuth("ROLE_CHEF");
        stubOrder(1L, OrderStatus.PREPARING);

        assertThat(service.canUpdateStatus(1L, auth)).isTrue();
    }

    @Test
    @DisplayName("CHEF cannot update READY order (not kitchen workflow)")
    void chef_cannotUpdate_ready() {
        Authentication auth = mockAuth("ROLE_CHEF");
        stubOrder(1L, OrderStatus.READY);

        assertThat(service.canUpdateStatus(1L, auth)).isFalse();
    }

    @Test
    @DisplayName("CHEF cannot update PENDING order")
    void chef_cannotUpdate_pending() {
        Authentication auth = mockAuth("ROLE_CHEF");
        stubOrder(1L, OrderStatus.PENDING);

        assertThat(service.canUpdateStatus(1L, auth)).isFalse();
    }

    // ── WAITER rules ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("WAITER can update READY order (READY → SERVED)")
    void waiter_canUpdate_ready() {
        Authentication auth = mockAuth("ROLE_WAITER");
        stubOrder(1L, OrderStatus.READY);

        assertThat(service.canUpdateStatus(1L, auth)).isTrue();
    }

    @Test
    @DisplayName("WAITER cannot update CONFIRMED order")
    void waiter_cannotUpdate_confirmed() {
        Authentication auth = mockAuth("ROLE_WAITER");
        stubOrder(1L, OrderStatus.CONFIRMED);

        assertThat(service.canUpdateStatus(1L, auth)).isFalse();
    }

    // ── MANAGER / ADMIN rules ─────────────────────────────────────────────────

    @Test
    @DisplayName("MANAGER can update any order status")
    void manager_canUpdateAny() {
        Authentication auth = mockAuth("ROLE_MANAGER");
        // No repository stub needed — manager is granted before the DB lookup.

        assertThat(service.canUpdateStatus(99L, auth)).isTrue();
        verify(orderRepository, never()).findById(any());
    }

    @Test
    @DisplayName("ADMIN can update any order status")
    void admin_canUpdateAny() {
        Authentication auth = mockAuth("ROLE_ADMIN");

        assertThat(service.canUpdateStatus(99L, auth)).isTrue();
        verify(orderRepository, never()).findById(any());
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("null authentication → denied")
    void nullAuthentication_denied() {
        assertThat(service.canUpdateStatus(1L, null)).isFalse();
    }

    @Test
    @DisplayName("CUSTOMER role → denied")
    void customer_denied() {
        Authentication auth = mockAuth("ROLE_CUSTOMER");
        stubOrder(1L, OrderStatus.READY);

        assertThat(service.canUpdateStatus(1L, auth)).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Authentication mockAuth(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getAuthorities()).thenAnswer(i ->
            List.of(new SimpleGrantedAuthority(role)));
        return auth;
    }

    private void stubOrder(Long id, OrderStatus status) {
        Order order = mock(Order.c
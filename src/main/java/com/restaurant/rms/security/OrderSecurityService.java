package com.restaurant.rms.security;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.Role;
import com.restaurant.rms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Custom security bean evaluated via SpEL in {@code @PreAuthorize} expressions.
 *
 * <p>Usage example in a controller:</p>
 * <pre>{@code
 * @PatchMapping("/{id}/status")
 * @PreAuthorize("@orderSecurityService.canUpdateStatus(#id, authentication)")
 * public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id, ...) { ... }
 * }</pre>
 *
 * <p>Business rules enforced here (not in the service layer) because these are
 * <em>access-control</em> decisions, not domain logic:</p>
 * <ul>
 *   <li>CHEF: may move orders from CONFIRMED → PREPARING → READY only.</li>
 *   <li>WAITER: may move orders from READY → SERVED only.</li>
 *   <li>MANAGER / ADMIN: unrestricted status transitions.</li>
 * </ul>
 */
@Service("orderSecurityService")   // bean name used in SpEL: @orderSecurityService
@RequiredArgsConstructor
@Slf4j
public class OrderSecurityService {

    // Statuses a CHEF is allowed to act on (kitchen workflow).
    private static final Set<OrderStatus> CHEF_ALLOWED_FROM = Set.of(
            OrderStatus.CONFIRMED,   // CONFIRMED → PREPARING
            OrderStatus.PREPARING    // PREPARING → READY
    );

    // Statuses a WAITER is allowed to act on (service workflow).
    private static final Set<OrderStatus> WAITER_ALLOWED_FROM = Set.of(
            OrderStatus.READY        // READY → SERVED
    );

    private final OrderRepository orderRepository;

    /**
     * Returns {@code true} if the currently authenticated user may update
     * the status of the order identified by {@code orderId}.
     *
     * <p>Called from {@code @PreAuthorize("@orderSecurityService.canUpdateStatus(#id, authentication)")}.</p>
     *
     * @param orderId        the PK of the order being updated
     * @param authentication the current Spring Security principal (injected by SpEL)
     */
    @Transactional(readOnly = true)
    public boolean canUpdateStatus(Long orderId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // MANAGER and ADMIN can change any order to any status.
        if (hasRole(authentication, Role.MANAGER) || hasRole(authentication, Role.ADMIN)) {
            return true;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            // Order not found — let the service layer throw 404; don't block here.
            log.warn("canUpdateStatus: order {} not found; granting access (service will 404)", orderId);
            return true;
        }

        OrderStatus current = order.getStatus();

        if (hasRole(authentication, Role.CHEF)) {
            // CHEF may only progress orders through the kitchen workflow states.
            boolean allowed = CHEF_ALLOWED_FROM.contains(current);
            if (!allowed) {
                log.warn("CHEF '{}' denied status update on order {} (current={})",
                        authentication.getName(), orderId, current);
            }
            return allowed;
        }

        if (hasRole(authentication, Role.WAITER)) {
            // WAITER may only mark a READY order as SERVED.
            boolean allowed = WAITER_ALLOWED_FROM.contains(current);
            if (!allowed) {
                log.warn("WAITER '{}' denied status update on order {} (current={})",
                        authentication.getName(), orderId, current);
            }
            return allowed;
        }

        // Any other role (CUSTOMER etc.) is denied.
        return false;
    }

    /**
     * Convenience helper: checks if {@code authentication} carries a given role.
     * Spring Security stores roles as "ROLE_CHEF", so we prepend the prefix.
     */
    private boolean hasRole(Authentication authentication, Role role) {
        String authority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}

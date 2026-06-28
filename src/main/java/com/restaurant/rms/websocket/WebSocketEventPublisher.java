package com.restaurant.rms.websocket;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Publishes typed WebSocket/STOMP events after order and table mutations.
 *
 * <p><strong>Transaction safety:</strong> these methods must be called
 * <em>after</em> the database transaction commits (use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} or call from the
 * service layer after {@code repository.save()} returns). A WebSocket push that
 * fires inside an open transaction can race with the commit and deliver stale data
 * to subscribers who immediately issue a REST fetch.</p>
 *
 * <p>All methods are intentionally non-transactional and must never throw —
 * a failed push must not roll back or block the calling business operation.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // ─── Topic destinations ────────────────────────────────────────────────────
    private static final String TOPIC_KITCHEN       = "/topic/kitchen";
    private static final String TOPIC_FLOOR         = "/topic/floor";
    private static final String TOPIC_ORDERS_PREFIX = "/topic/orders/";

    // ──────────────────────────────────────────────────────────────────────────
    //  New Order — pushed when a waiter creates an order
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts {@code order} to a {@link KitchenDisplayMessage} and broadcasts it
     * to {@code /topic/kitchen} so the KDS grid renders a new ticket card instantly.
     *
     * @param order the freshly persisted order (all items already attached)
     */
    public void publishNewOrder(Order order) {
        try {
            KitchenDisplayMessage msg = toKitchenMessage(order);
            messagingTemplate.convertAndSend(TOPIC_KITCHEN, msg);
            log.debug("WS → {} | new order id={} table={}",
                    TOPIC_KITCHEN, order.getId(),
                    order.getTable() != null ? order.getTable().getTableNumber() : "N/A");
        } catch (Exception ex) {
            log.error("Failed to publish new-order WS event for order id={}: {}",
                    order.getId(), ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Order Status Change — pushed on every status transition
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Broadcasts an {@link OrderUpdateMessage} to three topics simultaneously:
     * <ol>
     *   <li>{@code /topic/orders/{orderId}} — the waiter's per-order status badge</li>
     *   <li>{@code /topic/kitchen}          — the chef's KDS (e.g. to remove a COMPLETED card)</li>
     *   <li>{@code /topic/floor}            — floor manager's table grid (if table status changed)</li>
     * </ol>
     *
     * @param order order after status transition, {@code previousStatus} must be set by caller
     */
    public void publishOrderStatusChange(Order order) {
        publishOrderStatusUpdate(order, null);
    }

    /**
     * Overload that accepts the {@code previousStatus} for richer diff display.
     */
    public void publishOrderStatusUpdate(Order order, OrderStatus previousStatus) {
        try {
            String updatedBy = resolveCurrentUsername();
            OrderUpdateMessage updateMsg = OrderUpdateMessage.builder()
                    .orderId(order.getId())
                    .orderNumber("ORD-" + order.getId())
                    .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                    .previousStatus(previousStatus)
                    .status(order.getStatus())
                    .updatedAt(LocalDateTime.now())
                    .updatedBy(updatedBy)
                    .build();

            // 1. Waiter watching this specific order
            String orderTopic = TOPIC_ORDERS_PREFIX + order.getId();
            messagingTemplate.convertAndSend(orderTopic, updateMsg);

            // 2. Chef's kitchen display
            messagingTemplate.convertAndSend(TOPIC_KITCHEN, updateMsg);

            // 3. Floor manager's table overview
            messagingTemplate.convertAndSend(TOPIC_FLOOR, updateMsg);

            log.debug("WS → {}, {}, {} | order id={} status={}",
                    orderTopic, TOPIC_KITCHEN, TOPIC_FLOOR,
                    order.getId(), order.getStatus());
        } catch (Exception ex) {
            log.error("Failed to publish order-status WS event for order id={}: {}",
                    order.getId(), ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Table Status Change — pushed when a table becomes available/occupied etc.
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Broadcasts a {@link TableStatusMessage} to {@code /topic/floor} so the
     * floor manager's table grid tiles update their colour in real time.
     *
     * @param table the table entity after its status was updated and saved
     */
    public void publishTableStatusUpdate(RestaurantTable table) {
        try {
            TableStatusMessage msg = TableStatusMessage.builder()
                    .tableId(table.getId())
                    .tableNumber(table.getTableNumber())
                    .capacity(table.getCapacity())
                    .status(table.getStatus())
                    .locationDescription(table.getLocationDescription())
                    .build();
            messagingTemplate.convertAndSend(TOPIC_FLOOR, msg);
            log.debug("WS → {} | table #{} status={}", TOPIC_FLOOR,
                    table.getTableNumber(), table.getStatus());
        } catch (Exception ex) {
            log.error("Failed to publish table-status WS event for table id={}: {}",
                    table.getId(), ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Converts an Order entity to the full kitchen-display payload. */
    private KitchenDisplayMessage toKitchenMessage(Order order) {
        long minutesWaiting = order.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now())
                : 0;

        var items = order.getItems().stream()
                .map(oi -> KitchenDisplayMessage.KitchenItem.builder()
                        .orderItemId(oi.getId())
                        .name(oi.getMenuItem() != null ? oi.getMenuItem().getName() : "?")
                        .quantity(oi.getQuantity())
                        .notes(oi.getSpecialNotes())
                        .status(oi.getStatus().name())
                        .build())
                .toList();

        return KitchenDisplayMessage.builder()
                .orderId(order.getId())
                .orderNumber("ORD-" + order.getId())
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .status(order.getStatus())
                .items(items)
                .placedAt(order.getCreatedAt())
                .priority(minutesWaiting >= 10 ? "HIGH" : "NORMAL")
                .waiterName(order.getWaiter() != null ? order.getWaiter().getUsername() : "N/A")
                .build();
    }

    /** Extracts the authenticated username from the Security context. */
    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }
}

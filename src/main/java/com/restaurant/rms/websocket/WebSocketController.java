package com.restaurant.rms.websocket;

import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.repository.RestaurantTableRepository;
import com.restaurant.rms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Handles inbound STOMP frames (sent from browsers to {@code /app/**}) and
 * subscription-time snapshots (via {@code @SubscribeMapping}).
 *
 * <h3>@SubscribeMapping vs @MessageMapping</h3>
 * <ul>
 *   <li>{@code @SubscribeMapping} fires once, the moment a client subscribes.
 *       The return value is sent <em>directly back to that subscriber only</em>
 *       (not broadcast to the topic). It is ideal for sending an initial state
 *       snapshot so the UI doesn't show an empty screen while waiting for the
 *       first real event.</li>
 *   <li>{@code @MessageMapping} fires each time the client sends a frame to
 *       {@code /app/...}. The return value (if any) is sent to the "reply topic"
 *       but in this app we prefer explicit {@link WebSocketEventPublisher} calls
 *       so all listeners on multiple topics receive the update.</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RestaurantTableRepository tableRepository;
    private final WebSocketEventPublisher wsPublisher;

    // ──────────────────────────────────────────────────────────────────────────
    //  Inbound command — status update
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * A chef or floor manager sends a frame to {@code /app/order.updateStatus}.
     * We delegate to the service (which handles validation and inventory),
     * and the service's {@link WebSocketEventPublisher} call broadcasts the result
     * to all relevant topics.
     *
     * @param request contains {@code orderId} and target {@code status}
     */
    @MessageMapping("/order.updateStatus")
    public void handleStatusUpdate(@Payload OrderStatusUpdateRequest request) {
        if (request.getOrderId() == null || request.getStatus() == null) {
            log.warn("WS /order.updateStatus received malformed request — missing orderId or status");
            return;
        }
        try {
            OrderResponse updated = orderService.updateOrderStatus(
                    request.getOrderId(), request);
            log.info("WS /order.updateStatus | order id={} → status={}",
                    request.getOrderId(), request.getStatus());
        } catch (Exception ex) {
            log.error("WS /order.updateStatus failed for order id={}: {}",
                    request.getOrderId(), ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Subscription snapshots — initial state on connect
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * When a kitchen client subscribes to {@code /app/kitchen} (note: not
     * {@code /topic/kitchen}), it immediately receives the current list of
     * active orders as a JSON array. Subsequent live updates arrive via the
     * publisher's broadcast to {@code /topic/kitchen}.
     *
     * @return list of active-order kitchen messages sent only to the subscribing session
     */
    @SubscribeMapping("/kitchen")
    public List<KitchenDisplayMessage> kitchenSnapshot() {
        log.debug("WS kitchen snapshot requested");
        List<OrderStatus> active = List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.PREPARING, OrderStatus.READY);

        return orderRepository.findAll().stream()
                .filter(o -> active.contains(o.getStatus()))
                .map(order -> {
                    // lazy-load items are already in session here
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
                            .tableNumber(order.getTable() != null
                                    ? order.getTable().getTableNumber() : null)
                            .status(order.getStatus())
                            .items(items)
                            .placedAt(order.getCreatedAt())
                            .waiterName(order.getWaiter() != null
                                    ? order.getWaiter().getUsername() : "N/A")
                            .priority("NORMAL")
                            .build();
                })
                .toList();
    }

    /**
     * When a floor-manager client subscribes to {@code /app/floor}, it immediately
     * receives a snapshot of every table's current status. Live updates arrive via
     * {@code /topic/floor}.
     *
     * @return list of all table status messages, sent only to the subscribing session
     */
    @SubscribeMapping("/floor")
    public List<TableStatusMessage> floorSnapshot() {
        log.debug("WS floor snapshot requested");
        return tableRepository.findAll().stream()
                .map(t -> TableStatusMessage.builder()
                        .tableId(t.getId())
                        .tableNumber(t.getTableNumber())
                        .capacity(t.getCapacity())
                        .status(t.getStatus())
                        .locationDescription(t.getLocationDescription())
                        .build())
                .toList();
    }
}

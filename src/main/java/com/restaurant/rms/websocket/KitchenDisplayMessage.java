package com.restaurant.rms.websocket;

import com.restaurant.rms.entity.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order payload pushed to the Kitchen Display System (KDS) when a new order
 * is placed or an item status changes. Sent to {@code /topic/kitchen}.
 *
 * <p>This message is intentionally denormalized — the kitchen screen must render a
 * complete ticket without additional HTTP round-trips.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenDisplayMessage {

    private Long orderId;
    private String orderNumber;
    private Integer tableNumber;

    /** Current order status as of this push. */
    private OrderStatus status;

    /** Line items visible on the kitchen ticket. */
    private List<KitchenItem> items;

    /** When the order was first placed — used to compute elapsed time on the KDS. */
    private LocalDateTime placedAt;

    /**
     * Priority level: {@code HIGH} for orders waiting more than 10 minutes,
     * {@code NORMAL} otherwise (computed in the publisher).
     */
    private String priority;

    /** Name of the waiter who placed the order. */
    private String waiterName;

    // ──────────────────────────────────────────────────────────────────────────
    //  Nested type — one item on the kitchen ticket
    // ──────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KitchenItem {
        private Long orderItemId;
        private String name;
        private Integer quantity;
        privat
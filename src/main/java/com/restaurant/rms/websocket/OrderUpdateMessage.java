package com.restaurant.rms.websocket;

import com.restaurant.rms.entity.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lightweight status-change payload pushed to:
 * <ul>
 *   <li>{@code /topic/orders/{orderId}} — waiter watching a specific order</li>
 *   <li>{@code /topic/kitchen}          — chef's kitchen display system</li>
 *   <li>{@code /topic/floor}            — floor manager watching all tables</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateMessage {

    private Long orderId;

    /** Human-readable order identifier shown on receipts and tickets. */
    private String orderNumber;

    /** Physical table number (null for takeaway / delivery). */
    private Integer tableNumber;

    private OrderStatus previousStatus;
    private OrderStatus status;

    /** ISO-8601 timestamp of the status change. */
    private LocalDateTime updatedAt;

    /** Username of the staff member who triggered the change. 
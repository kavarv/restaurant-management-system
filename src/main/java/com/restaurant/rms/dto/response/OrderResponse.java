package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order detail response — includes items, waiter name, and table number
 * so the client has everything it needs without additional requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    /** Human-readable order number shown in the UI, e.g. "ORD-42". */
    private String orderNumber;
    private Long tableId;
    private Integer tableNumber;
    private Long waiterId;
    private String waiterName;
    private OrderStatus status;
    /** 0=PENDING … 5=COMPLETED, used by the stepper in orders/detail.html. */
    private int statusIndex;
    private OrderType orderType;
    private String notes;
    /** Pre-tax subtotal (totalAmount ÷ 1.10). */
    private BigDecimal subtotal;
    /** 10% tax component. */
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private List<OrderItemResponse> items;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        BigDecimal total    = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = total.divide(new BigDecimal("1.10"), 2, RoundingMode.HALF_UP);
        BigDecimal tax      = total.subtract(subtotal).setScale(2, RoundingMode.HALF_UP);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber("ORD-" + order.getId())
                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .waiterId(order.getWaiter() != null ? order.getWaiter().getId() : null)
                .waiterName(order.getWaiter() != null ? order.getWaiter().getUsername() : null)
                .status(order.getStatus())
                .statusIndex(statusIndex(order.getStatus()))
                .orderType(order.getOrderType())
                .notes(order.getNotes())
                .subtotal(subtotal)
                .taxAmount(tax)
                .totalAmount(total)
                .deliveryAddress(order.getDeliveryAddress())
                .items(order.getItems() != null
                        ? order.getItems().stream().map(OrderItemResponse::from).toList()
                        : List.of())
                .payment(order.getPayment() != null ? PaymentResponse.from(order.getPayment()) : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static int statusIndex(OrderStatus s) {
        if (s == null) return 0;
        return switch (s) {
            case PENDING   -> 0;
            case CONFIRMED -> 1;
            case PREPARING -> 2;
            case READY     -> 3;
            case SERVED    -> 4;
            case COMPLETED -> 5;
            default        -> 0;
        };
    }
}

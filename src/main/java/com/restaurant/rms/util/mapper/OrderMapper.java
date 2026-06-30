package com.restaurant.rms.util.mapper;

import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.Order;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for {@link Order} ↔ DTO conversions.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.from(order);
    }

    public OrderResponse toSummaryResponse(Order order) {
        // Summary omits items list — suitable for list views
        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTable() != null ? order.getTable().getId() : null)
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .waiterId(order.getWaiter() != null ? order.getWaiter().getId() : null)
                .waiterName(order.getWaiter() != null ? order.getWaiter().getUsername() : null)
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
     
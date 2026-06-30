package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    /** Required when the update arrives via WebSocket (no path variable). */
    private Long orderId;

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private St
package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.OrderItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderItemStatus 
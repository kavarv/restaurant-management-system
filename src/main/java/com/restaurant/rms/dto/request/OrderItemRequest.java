package com.restaurant.rms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * A single line item within a CreateOrderRequest.
 */
@Data
public class OrderItemRequest {

    @NotNull(message = "Menu item ID is required")
    @Positive(message = "Menu item ID must be a positive number")
    private Long menuItemId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 500, message = "Special notes must not exceed 500 characters")
    private String specialNotes;
}

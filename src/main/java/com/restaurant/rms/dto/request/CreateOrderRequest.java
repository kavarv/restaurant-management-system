package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request body for creating a new order")
public class CreateOrderRequest {

    @Positive(message = "Table ID must be a positive number")
    @Schema(description = "Restaurant table ID (required for DINE_IN, omit for TAKEAWAY/DELIVERY)", example = "5")
    private Long tableId;

    @NotNull(message = "Order type is required")
    @Schema(description = "Type of order", example = "DINE_IN", allowableValues = {"DINE_IN","TAKEAWAY","DELIVERY"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private OrderType orderType;

    @NotNull(message = "At least one item is required")
    @Size(min = 1, message = "Order must contain at least one item")
    @Valid
    @Schema(description = "List of ordered items (minimum 1)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemRequest> items;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Schema(description = "Special instructions for the kitchen", example = "No onions on table 5 please")
    private String notes;

    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    @Schema(description = "Delivery address (required for DELIVERY orders)", example = "123 Main Street, Apt 4B")
    private String deliveryAddress;

    @Schema(description = "Waiter user ID — populated server-side from the session; only set this if a manager is assigning to a specific waiter", example =
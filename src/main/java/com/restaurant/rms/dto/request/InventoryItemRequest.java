package com.restaurant.rms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Unit is required")
    @Size(max = 30)
    private String unit;

    @NotNull(message = "Current stock is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    private BigDecimal currentStock;

    @NotNull(message = "Minimum stock is required")
    @PositiveOrZero(message = "Minimum stock cannot be negative")
    private BigDecimal minimumStock;

    @NotNull(message = "Cost per unit is required")
    @Positive(message = "Cost must be greater than zero")
    private BigDecimal costPerUnit;

    @Size(max = 200)
    private String supplierName;
}

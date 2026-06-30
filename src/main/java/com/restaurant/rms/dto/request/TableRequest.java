package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.TableStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TableRequest {

    @NotNull(message = "Table number is required")
    @Positive(message = "Table number must be positive")
    private Integer tableNumber;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 50, message = "Capacity cannot exceed 50")
    private Integer capacity;

    private TableStatus status = TableStatus.AVAILABLE;

    @Size(max = 100)
    private String lo
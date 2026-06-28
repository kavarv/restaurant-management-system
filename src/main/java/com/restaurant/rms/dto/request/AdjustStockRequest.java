package com.restaurant.rms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustStockRequest {

    @NotNull(message = "Adjustment quantity is required")
    private BigDecimal quantity;   // positive = credit, negative = debit

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}

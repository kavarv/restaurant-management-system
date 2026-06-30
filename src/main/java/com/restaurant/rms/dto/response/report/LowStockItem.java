package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockItem {
    private Long inventoryItemId;
    private String name;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal minimumStock;
    private BigDecimal deficit;
    private String sup
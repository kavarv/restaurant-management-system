package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;

/** Single low-stock row for the low-stock report export. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockReportDTO {
    private Long inventoryItemId;
    private String itemName;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal reorderThreshold;
    private BigDecimal deficit;
    private String su
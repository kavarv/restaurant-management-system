package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockReport {
    private List<LowStockItem> items;
    private int totalLowStock
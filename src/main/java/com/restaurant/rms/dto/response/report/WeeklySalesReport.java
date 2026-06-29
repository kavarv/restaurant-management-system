package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklySalesReport {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private BigDecimal averageOrderValue;
    private List<DailySalesItem> dailyBreakdown;
}

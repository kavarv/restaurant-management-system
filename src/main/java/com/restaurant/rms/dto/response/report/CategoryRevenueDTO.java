package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;

/** Per-category revenue breakdown returned by the category-revenue report. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryRevenueDTO {
    private Long categoryId;
    private String categoryName;
    private Long orderCount;
    private BigDecimal totalRevenue;
    private Double percentag
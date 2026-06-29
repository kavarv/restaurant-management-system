package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategorySalesItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal revenue;
    private Long orderCount;
}

package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TopMenuItemReport {
    private Long menuItemId;
    private String menuItemName;
    private String categoryName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;
}

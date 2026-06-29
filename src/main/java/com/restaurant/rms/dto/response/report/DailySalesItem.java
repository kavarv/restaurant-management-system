package com.restaurant.rms.dto.response.report;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySalesItem {
    private LocalDate date;
    private BigDecimal revenue;
}

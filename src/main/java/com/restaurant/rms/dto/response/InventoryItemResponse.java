package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.InventoryItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItemResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal minimumStock;
    private BigDecimal costPerUnit;
    private String supplierName;
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryItemResponse from(InventoryItem i) {
        return InventoryItemResponse.builder()
                .id(i.getId())
                .name(i.getName())
                .unit(i.getUnit())
                .currentStock(i.getCurrentStock())
                .minimumStock(i.getMinimumStock())
                .costPerUnit(i.getCostPerUnit())
                .supplierName(i.getSupplierName())
                .lowStock(i.getCurrentStock().compareTo(i.getMinimumStock()) < 0)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}

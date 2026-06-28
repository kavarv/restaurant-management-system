package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.OrderItem;
import com.restaurant.rms.entity.enums.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long menuItemId;
    private String menuItemName;
    private String menuItemImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String specialNotes;
    private OrderItemStatus status;
    private Integer preparationTimeMinutes;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem() != null ? item.getMenuItem().getId() : null)
                .menuItemName(item.getMenuItem() != null ? item.getMenuItem().getName() : null)
                .menuItemImageUrl(item.getMenuItem() != null ? item.getMenuItem().getImageUrl() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .specialNotes(item.getSpecialNotes())
                .status(item.getStatus())
                .preparationTimeMinutes(item.getMenuItem() != null
                        ? item.getMenuItem().getPreparationTimeMinutes() : null)
                .build();
    }
}

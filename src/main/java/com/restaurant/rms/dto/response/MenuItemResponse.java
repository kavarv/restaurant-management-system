package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.MenuItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItemResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Boolean isAvailable;
    private Boolean isVegetarian;
    private Boolean isVegan;
    private Boolean isGlutenFree;
    private String imageUrl;
    private Integer preparationTimeMinutes;
    private Integer calories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Populated when the item has been soft-deleted — null for active items. */
    private LocalDateTime deletedAt;

    public static MenuItemResponse from(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .isAvailable(item.getIsAvailable())
                .isVegetarian(item.getIsVegetarian())
                .isVegan(item.getIsVegan())
                .isGlutenFree(item.getIsGlutenFree())
                .imageUrl(item.getImageUrl())
                .preparationTimeMinutes(item.getPreparationTimeMinutes())
                .calories(item.getCalories())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .deletedAt(item.getDeletedAt())
                .build();
    }
}

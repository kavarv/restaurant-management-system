package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.TableStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TableResponse {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus status;
    private String locationDescription;
    private LocalDateTime createdAt;

    public static TableResponse from(RestaurantTable t) {
        return TableResponse.builder()
                .id(t.getId())
                .tableNumber(t.getTableNumber())
                .capacity(t.getCapacity())
                .status(t.getStatus())
                .locationDescription(t.getLocationDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}

package com.restaurant.rms.websocket;

import com.restaurant.rms.entity.enums.TableStatus;
import lombok.*;

/**
 * Lightweight table-status change payload pushed to {@code /topic/floor}.
 * The floor manager's grid subscribes to this channel and updates tile colours
 * without a page reload: AVAILABLE=green, OCCUPIED=red, RESERVED=yellow,
 * MAINTENANCE=grey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatusMessage {

    private Long tableId;
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus status;
    private String lo
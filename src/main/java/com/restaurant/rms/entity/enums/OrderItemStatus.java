package com.restaurant.rms.entity.enums;

/**
 * Granular kitchen status for a single line item within an Order.
 * Allows the kitchen display system to track each dish independently.
 */
public enum OrderItemStatus {
    PENDING,
    PREPARING,
    READY,
    SERVED,
    C
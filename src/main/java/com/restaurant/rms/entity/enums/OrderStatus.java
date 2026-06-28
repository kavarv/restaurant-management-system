package com.restaurant.rms.entity.enums;

/**
 * Full lifecycle of an Order from placement to settlement.
 */
public enum OrderStatus {
    /** Order received but not yet acknowledged by staff. */
    PENDING,
    /** Order acknowledged and accepted by kitchen. */
    CONFIRMED,
    /** Kitchen is actively working on the order. */
    PREPARING,
    /** All items are ready for pickup or serving. */
    READY,
    /** Food has been delivered to the table. */
    SERVED,
    /** Order was cancelled before completion. */
    CANCELLED,
    /** Order fully served and payment collected. */
    COMPLETED
}

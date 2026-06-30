package com.restaurant.rms.entity.enums;

/**
 * Fulfillment channel for an Order.
 */
public enum OrderType {
    /** Guest is seated at a restaurant table. */
    DINE_IN,
    /** Guest orders and picks up themselves. */
    TAKEAWAY,
    /** Order delivered to an external address. */
    
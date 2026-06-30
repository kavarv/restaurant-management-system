package com.restaurant.rms.entity.enums;

/**
 * Lifecycle status of a physical restaurant table.
 */
public enum TableStatus {
    /** Table is clean and ready to accept new guests. */
    AVAILABLE,
    /** Table currently has seated guests. */
    OCCUPIED,
    /** Table is held for an upcoming reservation. */
    RESERVED,
    /** Table is taken out of service for cleaning or repairs. */
    M
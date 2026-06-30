package com.restaurant.rms.entity.enums;

/**
 * Application roles used for Spring Security authority checks.
 * Stored as VARCHAR in the database via @Enumerated(EnumType.STRING).
 */
public enum Role {
    ADMIN,
    MANAGER,
    WAITER,
    CHEF,
    
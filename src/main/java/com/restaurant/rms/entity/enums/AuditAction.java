package com.restaurant.rms.entity.enums;

/**
 * Type of mutation captured by an {@link com.restaurant.rms.entity.AuditLog} record.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    /** Soft-deleted record restored back to active state. */
    RESTORE
}

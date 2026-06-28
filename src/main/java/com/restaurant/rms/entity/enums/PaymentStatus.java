package com.restaurant.rms.entity.enums;

/**
 * Settlement status of a Payment transaction.
 */
public enum PaymentStatus {
    /** Payment initiated but gateway response not yet received. */
    PENDING,
    /** Payment authorized and funds captured. */
    COMPLETED,
    /** Gateway rejected the payment. */
    FAILED,
    /** Payment was reversed after initial completion. */
    REFUNDED
}

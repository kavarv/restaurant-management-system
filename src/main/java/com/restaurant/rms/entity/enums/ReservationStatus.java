package com.restaurant.rms.entity.enums;

/**
 * Lifecycle of a table Reservation.
 */
public enum ReservationStatus {
    /** Reservation created but not yet acknowledged by staff. */
    PENDING,
    /** Reservation acknowledged and the table is held. */
    CONFIRMED,
    /** Reservation cancelled by guest or staff. */
    CANCELLED,
    /** Guest arrived, dined, and departed; reservation closed successfully. */
    COMPLETED,
    /** Guest never arrived for a confirmed reservation. */
    NO_SHOW
}

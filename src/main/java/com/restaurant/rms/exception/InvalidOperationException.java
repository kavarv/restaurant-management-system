package com.restaurant.rms.exception;

/**
 * Thrown when a caller attempts an operation that is logically invalid given
 * the current state of the domain object.
 * Maps to HTTP 400 Bad Request (state conflict, not input validation).
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Cancelling an Order that is already in SERVED state.</li>
 *   <li>Adding items to a COMPLETED order.</li>
 *   <li>Confirming a reservation whose table is already OCCUPIED.</li>
 * </ul>
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String operation, String reason) {
        super(String.format("Cannot perform '%s': %s", operation, reason));
    }
}

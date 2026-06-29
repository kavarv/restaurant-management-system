package com.restaurant.rms.exception;

/**
 * Thrown when the authenticated user does not have permission to perform
 * the requested operation (role-based or ownership check).
 * Maps to HTTP 403 Forbidden.
 *
 * <p>Note: use this for authorisation failures (user is authenticated but
 * lacks permission).  Spring Security's {@code AccessDeniedException} is the
 * framework equivalent for filter-layer checks; this exception is for
 * business-logic-level authorisation in the service layer.</p>
 */
public class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String action, String resourceType) {
        super(String.format("You are not authorised to %s this %s", action, resourceType));
    }
}

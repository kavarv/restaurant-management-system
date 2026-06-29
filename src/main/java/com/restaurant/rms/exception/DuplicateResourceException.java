package com.restaurant.rms.exception;

/**
 * Thrown when a create/update operation would violate a uniqueness constraint.
 * Maps to HTTP 409 Conflict.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   throw new DuplicateResourceException("User", "email", email);
 *   // → "User already exists with email : 'bob@example.com'"
 * }</pre>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s : '%s'", resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}

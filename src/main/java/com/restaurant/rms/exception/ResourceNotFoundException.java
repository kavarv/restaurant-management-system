package com.restaurant.rms.exception;

/**
 * Thrown when a requested entity does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 *   throw new ResourceNotFoundException("MenuItem", "id", menuItemId);
 *   // → "MenuItem not found with id : '42'"
 * }</pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue() 
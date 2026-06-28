package com.restaurant.rms.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation of the {@link ValidTableCapacity} constraint.
 *
 * <p>The validator is instantiated once per validation factory and reused across
 * requests, so it must be stateless (aside from the annotation attributes
 * captured in {@link #initialize}).</p>
 *
 * <p>Null values are treated as valid — combine with {@code @NotNull} if null
 * should also be rejected.</p>
 */
public class TableCapacityValidator
        implements ConstraintValidator<ValidTableCapacity, Integer> {

    private int min;
    private int max;

    /**
     * Called once when the validator is first created.
     * Captures the {@code min} / {@code max} from the annotation instance.
     */
    @Override
    public void initialize(ValidTableCapacity annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    /**
     * @param value   the integer to validate (may be null)
     * @param context provides helpers to customise the violation message
     * @return {@code true} if the value is null (let @NotNull handle that)
     *         or falls within [min, max]; {@code false} otherwise.
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }
        if (value >= min && value <= max) {
            return true;
        }
        // Override the default message to include actual min/max values
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                String.format("Table capacity must be between %d and %d (provided: %d)",
                        min, max, value))
               .addConstraintViolation();
        return false;
    }
}

package com.restaurant.rms.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation constraint that ensures a table capacity is within
 * the restaurant's allowed range (1 – 20 inclusive).
 *
 * <h3>How a custom Bean Validation constraint works</h3>
 * <ol>
 *   <li>Define the annotation (this file) and mark it with {@code @Constraint},
 *       pointing at the validator implementation class.</li>
 *   <li>Implement {@link jakarta.validation.ConstraintValidator} in
 *       {@link TableCapacityValidator}.</li>
 *   <li>Apply {@code @ValidTableCapacity} to any field or parameter — the framework
 *       calls {@code TableCapacityValidator.isValid()} automatically.</li>
 * </ol>
 *
 * <p>The {@code message}, {@code groups}, and {@code payload} elements are
 * <em>required</em> by the Bean Validation specification for every constraint annotation.</p>
 */
@Documented
@Constraint(validatedBy = TableCapacityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTableCapacity {

    String message() default "Table capacity must be between 1 and 20";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Minimum allowed capacity. Override per use-site if needed. */
    int min() default 1;

    /** Maximum allowed capacity. Override per use-site if needed. */
    int max() default 20;
}

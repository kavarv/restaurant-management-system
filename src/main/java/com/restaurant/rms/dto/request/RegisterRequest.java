package com.restaurant.rms.dto.request;

import com.restaurant.rms.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for the POST /api/auth/register endpoint.
 *
 * <p>Validation strategy:
 * <ul>
 *   <li>{@code @NotBlank} — rejects null, empty, and whitespace-only strings.</li>
 *   <li>{@code @Size} — enforces min/max character lengths after trimming.</li>
 *   <li>{@code @Email} — validates RFC 5322 email format.</li>
 *   <li>{@code @Pattern} — enforces a phone number format (digits + optional +/- separators).</li>
 * </ul>
 * </p>
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username may only contain letters, digits, and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, "
                + "one digit, and one special character"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(
        regexp = "^(\\+\\d{1,3}[- ]?)?\\d{7,15}$",
        message = "Phone number is not valid"
    )
    private String phone;

    @NotNull(me
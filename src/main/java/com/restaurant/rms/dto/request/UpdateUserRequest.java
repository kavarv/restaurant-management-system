package com.restaurant.rms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Email(message = "Must be a valid email address")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^(\\+\\d{1,3}[- ]?)?\\d{7,15}$", message = "Phone number is not valid")
    private String phone;

    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
        message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    private String password;
}

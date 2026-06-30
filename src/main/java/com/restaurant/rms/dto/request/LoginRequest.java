package com.restaurant.rms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username or email address", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Str
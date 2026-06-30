package com.restaurant.rms.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consistent error envelope returned by {@link GlobalExceptionHandler} for every
 * error response.
 *
 * <p>JSON shape (fieldErrors only present for validation failures):</p>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "MenuItem not found with id : '99'",
 *   "path":      "/api/menu-items/99",
 *   "fieldErrors": {              ← only on 400 validation errors
 *     "price": "must be greater than 0",
 *     "name":  "must not be blank"
 *   }
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String error;
    private String message;
    private String path;

    /** Present only for {@code MethodArgumentNotValidException} (HTTP 400). */
 
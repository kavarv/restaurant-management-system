package com.restaurant.rms.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Central exception handling for all REST controllers.
 *
 * <p>Design decisions:</p>
 * <ul>
 *   <li>Uses {@code @RestControllerAdvice} (= {@code @ControllerAdvice} +
 *       {@code @ResponseBody}) so the return value is serialised as JSON automatically.</li>
 *   <li>Every handler builds an {@link ErrorResponse} via its builder so the JSON
 *       shape is always consistent.</li>
 *   <li>The generic {@code Exception} handler deliberately omits the stack trace from
 *       the response body to prevent information leakage; it logs the full trace at
 *       ERROR level for server-side diagnosis.</li>
 *   <li>The {@code path} field is extracted from {@link HttpServletRequest} so clients
 *       know exactly which endpoint triggered the error.</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "com.restaurant.rms.controller.api")
@Slf4j
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  Domain exceptions                                                   //
    // ------------------------------------------------------------------ //

    /**
     * 404 Not Found — entity does not exist.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    /**
     * 409 Conflict — uniqueness violation.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    /**
     * 422 Unprocessable Entity — stock level too low to fulfil the request.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {

        log.warn("Insufficient stock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request));
    }

    /**
     * 400 Bad Request — logically invalid state transition or domain rule violation.
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {

        log.warn("Invalid operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    /**
     * 403 Forbidden — authenticated user lacks permission for this operation.
     */
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOperation(
            UnauthorizedOperationException ex, HttpServletRequest request) {

        log.warn("Unauthorized operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }

    // ------------------------------------------------------------------ //
    //  Bean Validation                                                     //
    // ------------------------------------------------------------------ //

    /**
     * 400 Bad Request — {@code @Valid} failed on a request DTO.
     *
     * <p>Iterates all {@link FieldError}s and builds a map of
     * {@code fieldName → errorMessage} for the client to display per-field hints.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = (err instanceof FieldError fe) ? fe.getField() : err.getObjectName();
            String message = err.getDefaultMessage();
            // If a field has multiple violations, keep the last one (arbitrary choice;
            // could be changed to concatenate instead).
            fieldErrors.put(field, message);
        });

        log.warn("Validation failed: {} error(s)", fieldErrors.size());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ------------------------------------------------------------------ //
    //  Catch-all                                                           //
    // ------------------------------------------------------------------ //

    /**
     * 500 Internal Server Error — any unexpected runtime exception.
     *
     * <p>The full stack trace is written to the server log at ERROR level, but
     * only a safe generic message is returned to the client to prevent
     * information leakage (stack traces can expose library versions, file paths,
     * and internal class names that aid attackers).</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please try again later.", request));
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                              //
    // ------------------------------------------------------------------ //

    private ErrorResponse error(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}

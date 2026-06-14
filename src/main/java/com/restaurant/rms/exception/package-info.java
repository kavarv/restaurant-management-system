/**
 * Custom exception hierarchy and global error handling.
 *
 * <p>Planned classes:
 * <ul>
 *   <li><b>ResourceNotFoundException</b>   – 404 when an entity is not found by ID</li>
 *   <li><b>DuplicateResourceException</b>  – 409 when a unique-constraint violation occurs</li>
 *   <li><b>BusinessRuleException</b>       – 422 for domain rule violations (e.g., close an already-paid order)</li>
 *   <li><b>GlobalExceptionHandler</b>      – {@code @RestControllerAdvice} that maps exceptions
 *                                            to structured {@code ErrorResponse} JSON payloads</li>
 * </ul>
 *
 * <p>All exceptions extend {@code RuntimeException} so they propagate through
 * {@code @Transactional} boundaries and trigger rollback automatically.
 */
package com.restaurant.rms.exception;

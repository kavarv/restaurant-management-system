/**
 * Inbound DTO classes — data arriving FROM the client (form submissions, JSON bodies).
 *
 * <p>Rules:
 * <ul>
 *   <li>Validate with Bean Validation annotations ({@code @NotBlank}, {@code @Size}, etc.)</li>
 *   <li>NEVER expose entity IDs that the client should not control (use explicit fields)</li>
 *   <li>Name pattern: {@code <Entity>CreateRequest}, {@code <Entity>UpdateRequest}</li>
 * </ul>
 */
package com.restaurant.rms.dto.request;

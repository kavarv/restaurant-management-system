/**
 * Outbound DTO classes — data sent TO the client (JSON responses, Thymeleaf model objects).
 *
 * <p>Rules:
 * <ul>
 *   <li>Never return raw entity objects from controllers — always map to a response DTO
 *       to control what fields are exposed and avoid lazy-loading surprises</li>
 *   <li>Name pattern: {@code <Entity>Response}, {@code <Entity>Summary}</li>
 *   <li>Use {@code @JsonInclude(NON_NULL)} to omit null fields from JSON output</li>
 * </ul>
 */
package com.restaurant.rms.dto.response;

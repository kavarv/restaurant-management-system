/**
 * Service layer — business logic interfaces.
 *
 * <p>Define one interface per aggregate root (e.g., {@code OrderService},
 * {@code MenuService}).  Concrete implementations live in the {@code impl}
 * sub-package.  Separating interface from implementation:
 * <ul>
 *   <li>Allows easy swapping of implementations (e.g., caching decorator)</li>
 *   <li>Makes mocking trivial in unit tests</li>
 *   <li>Enforces the Dependency Inversion Principle</li>
 * </ul>
 */
package com.restaurant.rms.service;

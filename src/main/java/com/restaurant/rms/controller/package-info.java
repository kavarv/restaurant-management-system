/**
 * MVC and REST controllers, organised by role/domain.
 *
 * <p>Sub-packages:
 * <ul>
 *   <li><b>auth</b>    – login, logout, registration, password-reset pages</li>
 *   <li><b>admin</b>   – user management, role assignment, system settings</li>
 *   <li><b>waiter</b>  – table selection, order taking, bill request</li>
 *   <li><b>chef</b>    – Kitchen Display System (KDS) — view and update order status</li>
 *   <li><b>manager</b> – reports, menu management, shift overview</li>
 *   <li><b>api</b>     – REST endpoints consumed by the front-end / WebSocket clients</li>
 * </ul>
 *
 * <p>Controllers return Thymeleaf view names (Strings) for page controllers
 * and {@code ResponseEntity<?>} for API controllers.
 */
package com.restaurant.rms.controller;

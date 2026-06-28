package com.restaurant.rms.security;

/**
 * Part E — Method-Level Security: {@code @PreAuthorize} usage guide.
 *
 * <p>This file is a <strong>reference document, not a runnable class</strong>.
 * Copy the annotations shown below to the corresponding controller/service methods.
 * {@code @EnableMethodSecurity(prePostEnabled = true)} is already active in
 * {@link com.restaurant.rms.config.SecurityConfig}.</p>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ 1. Restrict a method to ADMIN only                                       │
 * │                                                                         │
 * │   {@literal @}PreAuthorize("hasRole('ADMIN')")                                   │
 * │   {@literal @}DeleteMapping("/api/v1/users/{id}")                               │
 * │   public ResponseEntity<Void> deleteUser({@literal @}PathVariable Long id) { … }│
 * │                                                                         │
 * │   Why: "hasRole('ROLE_X')" is shorthand for                            │
 * │   "hasAuthority('ROLE_X')" — Spring prepends ROLE_ automatically.       │
 * │                                                                         │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ 2. Allow MANAGER or ADMIN                                               │
 * │                                                                         │
 * │   {@literal @}PreAuthorize("hasAnyRole('MANAGER','ADMIN')")                     │
 * │   {@literal @}GetMapping("/api/v1/reports/daily-sales")                         │
 * │   public DailySalesReport getDailySales() { … }                        │
 * │                                                                         │
 * │   Also used on inventory management:                                    │
 * │   {@literal @}PreAuthorize("hasAnyRole('MANAGER','ADMIN')")                     │
 * │   {@literal @}PostMapping("/api/v1/inventory/{id}/adjust-stock")                │
 * │   public InventoryItemResponse adjustStock(…) { … }                    │
 * │                                                                         │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ 3. Users can only edit their own profile (or ADMIN can edit any)        │
 * │                                                                         │
 * │   {@literal @}PreAuthorize("#username == authentication.name or hasRole('ADMIN')")│
 * │   {@literal @}PutMapping("/api/v1/users/{username}")                            │
 * │   public UserResponse updateProfile(                                    │
 * │       {@literal @}PathVariable String username, …) { … }                       │
 * │                                                                         │
 * │   SpEL explanation:                                                     │
 * │   • #username  — binds to the @PathVariable named "username"           │
 * │   • authentication.name — the logged-in user's username from the context│
 * │   • hasRole('ADMIN') — bypass for admins                               │
 * │                                                                         │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ 4. Custom security bean — order status transitions                      │
 * │                                                                         │
 * │   {@literal @}PreAuthorize("@orderSecurityService.canUpdateStatus(#id, authentication)")│
 * │   {@literal @}PatchMapping("/api/v1/orders/{id}/status")                        │
 * │   public ResponseEntity<OrderResponse> updateStatus(                    │
 * │       {@literal @}PathVariable Long id,                                         │
 * │       {@literal @}RequestBody OrderStatusUpdateRequest req) { … }               │
 * │                                                                         │
 * │   SpEL explanation:                                                     │
 * │   • @orderSecurityService — Spring bean name (from {@literal @}Service("..."})  │
 * │   • #id — binds to the @PathVariable Long id                           │
 * │   • authentication — the current Authentication object (injected by     │
 * │     Spring Security's SpEL evaluation context)                         │
 * │   Business rules in OrderSecurityService:                               │
 * │   • CHEF: CONFIRMED→PREPARING or PREPARING→READY                       │
 * │   • WAITER: READY→SERVED                                               │
 * │   • MANAGER/ADMIN: any transition                                      │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>How evaluation errors surface:</p>
 * <ul>
 *   <li>If the expression evaluates to {@code false}, Spring throws
 *       {@link org.springframework.security.access.AccessDeniedException}, which
 *       our {@code accessDeniedHandler} in SecurityConfig catches and converts to
 *       403 JSON (REST) or a redirect to /403 (MVC).</li>
 *   <li>If the expression throws a runtime exception (e.g. bean not found),
 *       Spring wraps it in an {@link IllegalArgumentException}.  Always test
 *       SpEL expressions with at least one unit test (see OrderSecurityServiceTest).</li>
 * </ul>
 */
public final class MethodSecurityExamples {
    // Reference document only — no instances.
    private MethodSecurityExamples() {}
}

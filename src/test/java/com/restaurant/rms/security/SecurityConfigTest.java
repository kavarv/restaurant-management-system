package com.restaurant.rms.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link com.restaurant.rms.config.SecurityConfig}.
 *
 * <p>Uses {@code @SpringBootTest} + {@code @AutoConfigureMockMvc} so the full
 * security filter chain is exercised — not just the controller layer.</p>
 *
 * <p>Key annotations:</p>
 * <ul>
 *   <li>{@code @WithMockUser} — injects a synthetic {@link org.springframework.security.core.Authentication}
 *       into the SecurityContext without hitting the database.  Fastest for unit-style checks.</li>
 *   <li>{@code @WithUserDetails} — calls the real {@link UserDetailsService} to build the principal.
 *       Use this when the test depends on fields inside {@link UserPrincipal} (e.g. {@code id}, {@code role}).</li>
 *   <li>{@code csrf()} — attaches a valid CSRF token to requests that change state.
 *       Without it, POST/PUT/PATCH/DELETE returns 403 Forbidden from the CSRF filter.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================================
    // Public endpoints — should be accessible without authentication
    // =========================================================================

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /login is accessible anonymously")
        void loginPageIsPublic() throws Exception {
            mockMvc.perform(get("/login"))
                   .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /css/main.css is accessible anonymously")
        void staticResourceIsPublic() throws Exception {
            mockMvc.perform(get("/css/main.css"))
                   // 200 if file exists, 404 if not, but NOT 302 to /login
                   .andExpect(status().is(not(302)));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /api/v1/auth/csrf is accessible anonymously")
        void csrfEndpointIsPublic() throws Exception {
            mockMvc.perform(get("/api/v1/auth/csrf"))
                   .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // Unauthenticated access — should redirect MVC, return 401 for REST
    // =========================================================================

    @Nested
    @DisplayName("Unauthenticated access")
    class UnauthenticatedAccess {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /admin/dashboard redirects anonymous user to /login (MVC)")
        void adminDashboardRedirectsToLogin() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /api/v1/orders returns 401 JSON for unauthenticated REST client")
        void apiOrdersReturns401ForAnonymous() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                       .header("Accept", "application/json"))
                   .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Role-based authorization — ADMIN
    // =========================================================================

    @Nested
    @DisplayName("ADMIN role")
    class AdminRole {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can access /admin/dashboard")
        void adminCanAccessDashboard() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                   .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can access /inventory")
        void adminCanAccessInventory() throws Exception {
            mockMvc.perform(get("/inventory"))
                   .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can access /reports")
        void adminCanAccessReports() throws Exception {
            mockMvc.perform(get("/reports"))
                   .andExpect(status().is(not(403)));
        }
    }

    // =========================================================================
    // Role-based authorization — WAITER
    // =========================================================================

    @Nested
    @DisplayName("WAITER role")
    class WaiterRole {

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("WAITER is forbidden from /admin/**")
        void waiterCannotAccessAdmin() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("WAITER is forbidden from /inventory")
        void waiterCannotAccessInventory() throws Exception {
            mockMvc.perform(get("/inventory"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("WAITER can GET /orders")
        void waiterCanAccessOrders() throws Exception {
            mockMvc.perform(get("/orders"))
                   .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("WAITER can GET /orders/new")
        void waiterCanAccessNewOrderPage() throws Exception {
            mockMvc.perform(get("/orders/new"))
                   .andExpect(status().is(not(403)));
        }
    }

    // =========================================================================
    // Role-based authorization — CHEF
    // =========================================================================

    @Nested
    @DisplayName("CHEF role")
    class ChefRole {

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("CHEF can access /kitchen")
        void chefCanAccessKitchen() throws Exception {
            mockMvc.perform(get("/kitchen"))
                   .andExpect(status().is(not(403)));
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("CHEF is forbidden from /admin")
        void chefCannotAccessAdmin() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("CHEF cannot create new orders (POST /orders)")
        void chefCannotPostOrders() throws Exception {
            mockMvc.perform(post("/orders").with(csrf()))
                   .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // CSRF protection
    // =========================================================================

    @Nested
    @DisplayName("CSRF protection")
    class CsrfProtection {

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("POST /orders without CSRF token returns 403")
        void postWithoutCsrfTokenIsForbidden() throws Exception {
            // No .with(csrf()) — Spring Security's CSRF filter rejects this.
            mockMvc.perform(post("/orders"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("POST /orders WITH valid CSRF token passes CSRF filter")
        void postWithCsrfTokenPassesCsrfFilter() throws Exception {
            // .with(csrf()) injects _csrf param so the CSRF filter is satisfied.
            // The actual handler may still 400/404 if request body is invalid.
            mockMvc.perform(post("/orders").with(csrf()))
                   .andExpect(status().is(not(403)));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /api/v1/auth/login is CSRF-exempt (no token needed)")
        void apiLoginIsCsrfExempt() throws Exception {
            // /api/v1/auth/** is excluded from CSRF in SecurityConfig.
            mockMvc.perform(post("/api/v1/auth/login")
                       .contentType("application/json")
                       .content("{\"username\":\"x\",\"password\":\"y\"}"))
                   // 401 Bad Credentials, NOT 403 CSRF forbidden
                   .andExpect(status().is(not(403)));
        }
    }

    // =========================================================================
    // Session management — concurrent session control
    // =========================================================================

    @Nested
    @DisplayName("Session management")
    class SessionManagement {

        @Test
        @WithMockUser
        @DisplayName("Authenticated user can access protected resource")
        void authenticatedUserCanAccess() throws Exception {
            mockMvc.perform(get("/orders"))
                   .andExpect(status().is(not(401)))
                   .andExpect(status().is(not(302)));   // no redirect to /login
        }
    }

    // =========================================================================
    // Logout
    // =========================================================================

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @WithMockUser
        @DisplayName("POST /logout redirects to /lo
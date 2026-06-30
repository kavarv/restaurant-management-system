package com.restaurant.rms.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirects the user to a role-specific landing page after a successful login.
 *
 * <p>Extends {@link SavedRequestAwareAuthenticationSuccessHandler} so that if the
 * user was redirected to {@code /login} from a protected URL, they are sent back
 * to that originally-requested URL instead of the role default.  The role default
 * is only used when there is no saved request (e.g. user navigated directly to
 * {@code /login}).</p>
 *
 * <p>Role-to-URL mapping:</p>
 * <ul>
 *   <li>ADMIN    → /admin/dashboard</li>
 *   <li>MANAGER  → /manager/dashboard</li>
 *   <li>WAITER   → /orders</li>
 *   <li>CHEF     → /kitchen</li>
 *   <li>CUSTOMER → /menu</li>
 * </ul>
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication)
            throws ServletException, IOException {

        // Determine the role-appropriate landing page.
        String targetUrl = resolveTargetUrl(authentication);

        // setDefaultTargetUrl() is inherited — SavedRequestAwareAuthenticationSuccessHandler
        // will use this ONLY when there is no saved request to honour.
        setDefaultTargetUrl(targetUrl);

        // Always use the role-based URL — prevents Spring from honouring a
        // saved /error request that was captured before login.
        setAlwaysUseDefaultTargetUrl(true);

        log.info("Successful login: user='{}' role='{}' → default target='{}'",
                authentication.getName(),
                authentication.getAuthorities(),
                targetUrl);

        // Delegate to the parent to handle the saved-request check and actual redirect.
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Maps the authenticated user's single role to their home URL.
     * Returns {@code /dashboard} as a safe fallback for unexpected roles.
     */
    private String resolveTargetUrl(Authentication authentication) {
        // getAuthorities() returns "ROLE_<ROLENAME>" strings.
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .map(this::urlForAuthority)
                .orElse("/dashboard");
    }

    private String urlForAuthority(String authority) {
        return switch (authority) {
            case "ROLE_ADMIN"    -> "/admin/dashboard";
            case "ROLE_MANAGER"  -> "/manager/dashboard";
            case "ROLE_WAITER"   -> "/orders";
            case "ROLE_CHEF"     -> "/kitchen";
            case "ROLE_CUSTOMER" -> "/menu";
            def
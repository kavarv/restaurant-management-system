package com.restaurant.rms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

/**
 * Called by Spring Security whenever a login attempt fails.
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Increments the failure counter in {@link LoginAttemptService} so that
 *       subsequent calls to {@link LoginAttemptService#isBlocked(String)} in
 *       {@link UserDetailsServiceImpl} will eventually throw
 *       {@link LockedException}.</li>
 *   <li>For API requests ({@code Accept: application/json}) returns a JSON error
 *       body instead of a redirect, matching the REST contract used by the
 *       Postman collection and the Thymeleaf AJAX login form.</li>
 *   <li>For form-based requests redirects to {@code /login?error} (standard
 *       Spring Security convention).</li>
 * </ol>
 *
 * <p>The identifier used as the throttle key is the username value submitted in
 * the form.  In production you may want to key on both username AND remote IP
 * to prevent username-enumeration via lockout status.</p>
 */
@Component
@Slf4j
public class CustomAuthenticationFailureHandler
        extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final ObjectMapper        objectMapper;

    public CustomAuthenticationFailureHandler(LoginAttemptService loginAttemptService,
                                              ObjectMapper objectMapper) {
        super("/login?error");    // default redirect for form-based logins
        this.loginAttemptService = loginAttemptService;
        this.objectMapper        = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");

        if (username != null && !username.isBlank()) {
            loginAttemptService.registerFailure(username.trim().toLowerCase());
            int count = loginAttemptService.failureCount(username.trim().toLowerCase());
            int remaining = Math.max(0, LoginAttemptService.MAX_ATTEMPTS - count);
            log.warn("Login failed for '{}' — {} attempt(s) remaining before lock", username, remaining);
        }

        // JSON response for REST / AJAX callers.
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            boolean locked = exception instanceof LockedException;
            Map<String, Object> body = Map.of(
                    "status",  401,
                    "error",   locked ? "Account locked" : "Bad credentials",
                    "message", locked
                            ? "Too many failed attempts. Please try again in 15 minutes."
                            : "Invalid username or password.",
                    "path",    request.getRequestURI()
            );
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        // Fall through to the default redirect (/login?error) for form-based
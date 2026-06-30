package com.restaurant.rms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.security.CustomAuthenticationFailureHandler;
import com.restaurant.rms.security.CustomAuthenticationSuccessHandler;
import com.restaurant.rms.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Map;

/**
 * Central Spring Security configuration.
 *
 * <p>Design decisions:</p>
 * <ul>
 *   <li>Component-based config (no deprecated {@code WebSecurityConfigurerAdapter}).</li>
 *   <li>Session/cookie auth — no JWT.  JSESSIONID is HttpOnly + Secure + SameSite=Strict
 *       (configured in application-prod.properties).</li>
 *   <li>CSRF enabled globally; disabled only for {@code /api/v1/auth/**} because those REST
 *       endpoints use stateless login (no prior session) and a CSRF cookie cannot be set
 *       before the first request.</li>
 *   <li>Authorization rules are ordered most-specific to least-specific.  Spring Security
 *       evaluates rules top-to-bottom and stops at the first match.</li>
 *   <li>Brute-force protection: {@link CustomAuthenticationFailureHandler} increments a
 *       per-username counter in {@link com.restaurant.rms.security.LoginAttemptService}
 *       on each failure; {@link UserDetailsServiceImpl} checks the counter before running
 *       the BCrypt comparison and throws {@link org.springframework.security.authentication.LockedException}
 *       when the threshold is exceeded.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl             userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final ObjectMapper                       objectMapper;

    // =========================================================================
    // Password encoding
    // =========================================================================

    /**
     * BCrypt strength 12 — ~250 ms/hash, satisfies OWASP and NIST SP 800-63B.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // =========================================================================
    // Authentication provider
    // =========================================================================

    /**
     * Not a @Bean — kept as a private factory method so Spring Boot's security
     * auto-configuration does NOT pick it up and register a second provider.
     * Exposing it as a @Bean would cause loadUserByUsername to be called twice
     * per login attempt (once by our provider, once by the auto-configured one),
     * doubling brute-force counter increments.
     */
    private DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Masks UsernameNotFoundException as BadCredentialsException to prevent
        // username-enumeration: the attacker cannot tell whether the account exists.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // =========================================================================
    // Session-registry bean
    // =========================================================================

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // =========================================================================
    // Security filter chain
    // =========================================================================

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authenticationProvider(authenticationProvider())

            // ── Session management ───────────────────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )

            // ── Form login ───────────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)   // records failures; clears on success
                .permitAll()
            )

            // ── Logout ───────────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )

            // ── CSRF ─────────────────────────────────────────────────────────────
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/v1/auth/**")
            )

            // ── Authorization rules (most-specific first) ────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(
                    "/", "/login", "/register",
                    "/css/**", "/js/**", "/images/**",
                    "/favicon.ico", "/webjars/**"
                ).permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Public read-only APIs for the landing page
                .requestMatchers(HttpMethod.GET, "/api/v1/menu/**", "/api/v1/categories/**",
                                  "/api/v1/tables/available").permitAll()
                // Anonymous table reservations from the public landing page
                .requestMatchers(HttpMethod.POST, "/api/v1/reservations").permitAll()
                .requestMatchers("/kitchen").hasAnyRole("CHEF", "MANAGER", "ADMIN")
                .requestMatchers("/orders/new").hasAnyRole("WAITER", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/orders").hasAnyRole("WAITER", "MANAGER", "ADMIN")
                .requestMatchers("/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status")
                    .hasAnyRole("CHEF", "WAITER", "MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )

            // ── Exception handling ───────────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getWriter(), Map.of(
                            "status",  401,
                            "error",   "Unauthorized",
                            "message", "Authentication required. Please log in.",
                            "path",    uri
                        ));
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(Media
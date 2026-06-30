package com.restaurant.rms.security;

import com.restaurant.rms.entity.User;
import com.restaurant.rms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primary Spring Security {@link UserDetailsService}.
 *
 * <p>Supports login by <em>username OR email</em> — the submitted value is tried
 * against the username column first; if not found it falls back to email.  This
 * lets the same login form accept either identifier without the user knowing
 * which one they registered with.</p>
 *
 * <p>Returns a {@link UserPrincipal} (not the bare Spring {@code User} builder
 * object) so that controllers can access {@code id}, {@code email}, and
 * {@code role} via {@code @AuthenticationPrincipal UserPrincipal}.</p>
 *
 * <p><strong>Brute-force protection (S14):</strong> Before returning a principal,
 * {@link LoginAttemptService#isBlocked(String)} is checked.  If the identifier
 * has exceeded {@link LoginAttemptService#MAX_ATTEMPTS} consecutive failures,
 * a {@link LockedException} is thrown.  Spring Security then invokes
 * {@link CustomAuthenticationFailureHandler} which records the failure and
 * returns an appropriate 401 response.</p>
 */
@Service("userDetailsService")   // explicit bean name avoids ambiguity with CustomUserDetailsService
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository     userRepository;
    private final LoginAttemptService loginAttemptService;

    /**
     * Called by Spring Security on every authentication attempt.
     *
     * @param usernameOrEmail the value the user typed into the login form's
     *                        "username" field — may be a username OR an email address
     * @throws UsernameNotFoundException if no active account matches the identifier
     * @throws LockedException           if the identifier has exceeded the brute-force threshold
     */
    @Override
    @Transactional(readOnly = true)   // readOnly = true → Hibernate skip dirty-checking on flush
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        // Normalise the key used for throttle checks.
        String key = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase();

        // ── Brute-force check ─────────────────────────────────────────────────
        // isBlocked() returns true when ≥ MAX_ATTEMPTS failures have been recorded
        // since the last scheduled reset.  Throwing LockedException here prevents
        // the password-hash comparison from even running, which avoids the BCrypt
        // timing side-channel during a brute-force burst.
        if (loginAttemptService.isBlocked(key)) {
            log.warn("Login blocked for '{}': too many failed attempts", usernameOrEmail);
            throw new LockedException(
                    "Account temporarily locked due to too many failed login attempts. "
                    + "Please try again in 15 minutes.");
        }

        // 1. Try username first (most common path; indexed column).
        User user = userRepository.findByUsername(usernameOrEmail)
                // 2. Fall back to email — allows "login with email" without a separate endpoint.
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> {
                    log.warn("Authentication failed: no user found for identifier '{}'", usernameOrEmail);
                    // Deliberately vague message — don't leak whether username vs email was wrong.
                    return new UsernameNotFoundException(
                            "No account found for: " + usernameOrEmail);
                });

        log.debug("Loaded UserPrincipal for username='{}' role='{}'", user.getUsername(), user.getRole());
        // Wrap into a rich principal that exposes id, email,
package com.restaurant.rms.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks failed login attempts per username/IP and locks out accounts that
 * exceed the threshold.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>Uses a {@link ConcurrentHashMap} so concurrent login requests from different
 *       threads are safe without external locking.</li>
 *   <li>A scheduled job clears all counters every {@code RESET_INTERVAL_MS} (15 minutes),
 *       so a genuinely forgotten password auto-unblocks without admin intervention.</li>
 *   <li>For production, replace the in-memory map with Redis to survive restarts and
 *       support multiple application instances behind a load balancer.</li>
 * </ul>
 *
 * <p>Integration: {@link UserDetailsServiceImpl} calls {@link #isBlocked(String)}
 * before processing credentials, and the Spring Security
 * {@link org.springframework.security.web.authentication.AuthenticationFailureHandler}
 * (registered in {@code SecurityConfig}) calls {@link #registerFailure(String)} on
 * each failed attempt.</p>
 */
@Service
@Slf4j
public class LoginAttemptService {

    /** Max consecutive failures before the account/IP is locked out. */
    public static final int MAX_ATTEMPTS = 5;

    /** Lock-out window duration in milliseconds (15 minutes). */
    private static final long RESET_INTERVAL_MS = 15 * 60 * 1000;

    /**
     * Map of identifier (username or IP) → failure count.
     * {@link ConcurrentHashMap} chosen over {@code Collections.synchronizedMap} for
     * better throughput under concurrent reads.
     */
    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records one failed login attempt for the given key.
     *
     * @param key username, email, or remote IP (use whichever the caller has)
     */
    public void registerFailure(String key) {
        AtomicInteger count = attempts.computeIfAbsent(key, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        if (newCount >= MAX_ATTEMPTS) {
            log.warn("Login throttle: '{}' has reached {} failed attempts — blocking", key, newCount);
        }
    }

    /**
     * Resets the failure counter after a successful login so a legitimate user
     * who mistyped their password is not permanently locked out.
     *
     * @param key the same key passed to {@link #registerFailure(String)}
     */
    public void registerSuccess(String key) {
        attempts.remove(key);
        log.debug("Login throttle: cleared failure count for '{}'", key);
    }

    /**
     * Returns {@code true} if the key has exceeded {@link #MAX_ATTEMPTS} failures.
     *
     * <p>Called by {@link UserDetailsServiceImpl} — throwing
     * {@link org.springframework.security.authentication.LockedException} here causes
     * Spring Security to return 401 and invoke the failure handler, which calls
     * {@link #registerFailure(String)} — creating a natural throttle loop.</p>
     *
     * @param key username, email, or remote IP
     */
    public boolean isBlocked(String key) {
        AtomicInteger count = attempts.get(key);
        return count != null && count.get() >= MAX_ATTEMPTS;
    }

    /** Returns the current failure count for a key (used in tests and admin UI). */
    public int failureCount(String key) {
        AtomicInteger count = attempts.get(key);
        return count == null ? 0 : count.get();
    }

    // ── Scheduled reset ───────────────────────────────────────────────────────

    /**
     * Clears all attempt counters every 15 minutes.
     *
     * <p>This is a "fixed-rate" reset, not a per-key sliding window, which keeps the
     * implementation simple at the cost of slightly inconsistent lock-out durations
     * (a user blocked just after a reset gets a near-full 15-minute lock, while one
     * blocked just before gets almost no lock).  Acceptable for most deployments; use
     * a per-key expiry via Redis TTL if stricter timing is needed.</p>
     */
    @Scheduled(fixedRateString = "${rms.security.login-attempt-reset-interval-ms:900000}")
    public void resetAttempts() {
        int size = attempts.size();
        attempts.clear();
        if (size > 0) {
            log.info("Login throttle: cleared {} attempt counter(s) on scheduled reset", size);
        }
    }
}

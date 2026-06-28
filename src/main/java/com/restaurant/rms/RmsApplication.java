package com.restaurant.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <p>{@code @EnableScheduling} activates Spring's task scheduling infrastructure,
 * required by {@link com.restaurant.rms.security.LoginAttemptService} to
 * periodically reset brute-force attempt counters.</p>
 */
@SpringBootApplication
@EnableScheduling
public class RmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }
}

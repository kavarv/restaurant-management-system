package com.restaurant.rms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies that the Spring ApplicationContext loads without errors.
 * Run with: mvn test
 *
 * Uses the "test" profile so you can add src/test/resources/application-test.properties
 * with an H2 in-memory datasource later (avoids needing a running MySQL for CI).
 */
@SpringBootTest
@ActiveProfiles("dev")
class RmsApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails with a descriptive error.
        // No assertions needed — the test framework handles it.
    }
}

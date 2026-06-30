package com.restaurant.rms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class RmsApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without err
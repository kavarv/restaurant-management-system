package com.restaurant.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Restaurant Management System.
 * <p>
 * {@code @SpringBootApplication} is a convenience annotation that combines:
 * <ul>
 *   <li>{@code @Configuration}     – marks this class as a source of bean definitions</li>
 *   <li>{@code @EnableAutoConfiguration} – activates Spring Boot's auto-configuration</li>
 *   <li>{@code @ComponentScan}     – scans all sub-packages for Spring-managed components</li>
 * </ul>
 */
@SpringBootApplication
public class RmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }
}

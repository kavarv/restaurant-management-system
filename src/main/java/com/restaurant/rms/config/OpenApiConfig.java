package com.restaurant.rms.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 configuration.
 */
@Configuration
@SecurityScheme(
        name        = "cookieAuth",
        type        = SecuritySchemeType.APIKEY,
        in          = SecuritySchemeIn.COOKIE,
        paramName   = "JSESSIONID",
        description = "Session cookie issued by POST /api/v1/auth/login. " +
                      "Copy the JSESSIONID value from the Set-Cookie response header."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI restaurantManagementOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .tags(apiTags());
    }

    private Info apiInfo() {
        return new Info()
                .title("Restaurant Management System API")
                .version("1.0.0")
                .description("""
                        ## Overview
                        A full-stack restaurant operations platform built with Spring Boot 3.2.

                        ### Feature Areas
                        * **Menu & Categories** — item CRUD with soft-delete and restore; dietary flags
                        * **Orders** — full lifecycle PENDING→CONFIRMED→PREPARING→READY→SERVED→COMPLETED
                          with inventory auto-deduction and WebSocket events
                        * **Inventory** — stock tracking with low-stock alerts and manual adjustments
                        * **Reservations** — guest booking with table assignment and status workflow
                        * **Payments** — multiple payment methods, receipt generation
                        * **Reports** — daily/weekly revenue, category breakdown, CSV/PDF export
                        * **Kitchen Display** — real-time STOMP/WebSocket feed for KDS screen
                        * **Admin** — user management, role assignment, full audit log

                        ### Authentication
                        Call **POST /api/v1/auth/login** first, then use the JSESSIONID cookie.
                        For non-browser clients, call **GET /api/v1/auth/csrf** and pass the
                        token as `X-CSRF-TOKEN` on every state-changing request.
                        """)
                .contact(new Contact()
                        .name("RMS Development Team")
                        .email("socialkavi4@gmail.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag().name("auth")         .description("Authentication — login, logout, registration, CSRF token"),
                new Tag().name("admin")        .description("Administration — user management, audit log"),
                new Tag().name("menu")         .description("Menu items — create, read, update, soft-delete, restore"),
                new Tag().name("categories")   .description("Menu categories — full CRUD"),
                new Tag().name("orders")       .description("Orders — lifecycle management and item operations"),
                new Tag().name("inventory")    .description("Inventory — stock items, adjustments, low-stock alerts"),
                new Tag().name("reservations") .description("Reservations — booking, confirmation, cancellation"),
                new Tag().name("payments")     .description("Payments — processing and receipt retrieval"),
                new Tag().name("reports")      .description("Reports — sales, revenue, top items, CSV/PDF export"),
                new Tag().name("tables")       .description("Restaurant tables — status and capacity management")
        );
    }
}

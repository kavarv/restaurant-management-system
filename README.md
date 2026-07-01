# Restaurant Management System

A full-stack restaurant operations platform built with **Spring Boot 3.2** and **Java 17**. It handles the complete restaurant workflow: menu management, real-time order processing, kitchen display, inventory tracking, table reservations, billing, management reporting, and a customer-facing self-service panel — all from a single deployable JAR.

---

## Features

- **Customer Panel** — self-service portal for customers: browse the menu, make table reservations, and track their bookings after registering an account
- **Menu Management** — full CRUD for menu items and categories; soft-delete with restore; dietary flags (vegetarian, vegan, gluten-free); image URLs; preparation time and calorie tracking
- **Order Lifecycle** — state-machine transitions: PENDING → CONFIRMED → PREPARING → READY → SERVED → COMPLETED; automatic inventory deduction on create; inventory restore on cancel
- **Real-Time Kitchen Display (KDS)** — STOMP/WebSocket feed broadcasts new orders and status changes to the kitchen screen without polling
- **Inventory** — per-ingredient stock tracking with automatic deduction when orders are placed; low-stock alerts; manual stock adjustments with audit trail
- **Table Management** — floor-plan view with live status badges (AVAILABLE / OCCUPIED / RESERVED / MAINTENANCE)
- **Reservations** — guest booking with party size, date/time, and special requests; PENDING → CONFIRMED workflow
- **Payments & Billing** — cash, card, and UPI payment methods; PDF receipt generation; refund support
- **Reports** — daily and weekly sales reports; revenue by category; top-selling items; CSV and PDF export
- **Audit Log** — every create/update/delete records the acting user, timestamp, and before/after snapshot
- **Role-Based Access Control** — five roles: ADMIN, MANAGER, WAITER, CHEF, CUSTOMER; enforced at both HTTP and method level via `@PreAuthorize`
- **API Documentation** — interactive Swagger UI at `/swagger-ui.html`; full OpenAPI 3 spec at `/api-docs`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend framework | Spring Boot 3.2.5 (Java 17) |
| Web / REST | Spring MVC, Jackson |
| Frontend rendering | Thymeleaf 3 + Thymeleaf Security extras |
| Database | MySQL 8 (production), H2 (tests) |
| ORM | Spring Data JPA / Hibernate 6 |
| Security | Spring Security 6 — session/cookie auth, CSRF, BCrypt |
| WebSocket | Spring WebSocket + STOMP (SockJS fallback) |
| PDF export | iText 5 |
| CSV export | OpenCSV 5 |
| API docs | SpringDoc OpenAPI 2 (Swagger UI) |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| Build tool | Maven 3.9 |
| Dev tooling | Lombok, Spring Boot DevTools |

---

## Prerequisites

- Java 17 (JDK — not JRE): `java -version` should show `17.x.x`
- Maven 3.9+: `mvn -version`
- MySQL 8.0+: running locally on port 3306
- IntelliJ IDEA 2023+ (recommended) or VS Code with Java Extension Pack

---

## Setup Instructions

### 1. Clone the repository

```bash
git clone https://github.com/kavarv/restaurant-management-system.git
cd restaurant-management-system
```

### 2. Create the MySQL database and user

```sql
CREATE DATABASE restaurant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rms_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON restaurant_db.* TO 'rms_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure `application.properties`

Open `src/main/resources/application.properties` and update these values:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=rms_user
spring.datasource.password=your_secure_password
```

All other defaults work for local development. The schema is created automatically on first run via `schema.sql` (`spring.sql.init.mode=always`).

### 4. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts on **http://localhost:8080**. The first run populates default users and seed data via `DataInitializer`.

### 5. Access the application

| URL | Description |
|---|---|
| http://localhost:8080 | Landing page (public) |
| http://localhost:8080/login | Login page |
| http://localhost:8080/register | Customer self-registration |
| http://localhost:8080/customer/dashboard | Customer panel (requires CUSTOMER role) |
| http://localhost:8080/swagger-ui.html | Swagger UI (interactive API docs) |
| http://localhost:8080/api-docs | Raw OpenAPI 3 JSON |
| http://localhost:8080/actuator/health | Health check |

### 6. Default credentials

The following accounts are seeded automatically on first startup:

| Role | Username | Password | Access |
|---|---|---|---|
| Admin | `admin` | `Admin@123` | Full system access |
| Manager | `manager1` | `Manager@123` | Orders, inventory, reports |
| Waiter | `waiter1` | `Waiter@123` | Orders, tables |
| Waiter | `waiter2` | `Waiter@123` | Orders, tables |
| Chef | `chef1` | `Chef@123` | Kitchen display |

> **Customer accounts** are not seeded. Customers self-register at `/register` — a new account is automatically assigned the `CUSTOMER` role and gains access to the customer panel at `/customer/dashboard`.

---

## Customer Panel

Customers register at `/register` and are automatically assigned the `CUSTOMER` role. After login they can:

| URL | Description |
|---|---|
| `/customer/dashboard` | Personal dashboard with booking overview |
| `/customer/menu` | Browse the full menu with dietary filters |
| `/customer/reserve` | Make a new table reservation |
| `/customer/reservations` | View and manage their own reservations |

---

## Running Tests

```bash
# Run all unit tests (service + controller layer)
mvn test

# Run with coverage report (requires jacoco plugin)
mvn test jacoco:report
# Report generated at: target/site/jacoco/index.html

# Run a specific test class
mvn test -Dtest=MenuItemServiceTest

# Run integration tests (requires MySQL — disabled by default)
mvn test -Dtest=OrderIntegrationTest -DfailIfNoTests=false
```

Integration tests under `src/test/java/.../integration/` are annotated `@Disabled` and require a running MySQL instance with the `test` profile. Remove `@Disabled` to run them locally.

---

## API Documentation

Once the application is running, visit:

**http://localhost:8080/swagger-ui.html**

The Swagger UI lists all endpoints grouped by tag. To authenticate:

1. Call `POST /api/v1/auth/csrf` — copy the `token` value.
2. Call `POST /api/v1/auth/login` with your credentials. Copy `JSESSIONID` from the `Set-Cookie` response header.
3. Click the **Authorize** button (top-right in Swagger UI) and paste the `JSESSIONID` value.
4. All subsequent "Try it out" requests will include the session cookie.

---

## Postman Collection

Two files are included in the project root:

| File | Purpose |
|---|---|
| `RMS_Postman_Collection.json` | Complete collection (Auth, Menu, Orders, Inventory, Reservations, Payments, Reports, Admin) |
| `RMS_Environment.json` | Environment with `baseUrl`, `sessionId`, `csrfToken` variables |

**Import steps:**
1. Open Postman → **Import** → drag both JSON files.
2. Select the **RMS Local** environment from the environment dropdown.
3. Run **Auth → GET CSRF Token**, then **Auth → Login** — session variables are saved automatically by test scripts.
4. All other requests inherit `{{sessionId}}` and `{{csrfToken}}` from collection variables.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/restaurant/rms/
│   │   ├── config/          # SecurityConfig, WebSocketConfig, OpenApiConfig, DataInitializer
│   │   ├── controller/
│   │   │   ├── api/         # REST controllers (MenuItemApiController, OrderApiController, ...)
│   │   │   ├── CustomerController.java   # Customer panel (CUSTOMER role)
│   │   │   └── *.java       # Thymeleaf MVC controllers (views)
│   │   ├── dto/
│   │   │   ├── request/     # Incoming payloads with Bean Validation annotations
│   │   │   └── response/    # Outbound DTOs (never expose JPA entities directly)
│   │   ├── entity/          # JPA entities + enums (MenuItem, Order, OrderItem, ...)
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
│   │   ├── report/          # CsvExportService, PdfExportService
│   │   ├── repository/      # Spring Data JPA repositories with custom @Query methods
│   │   ├── security/        # UserPrincipal, UserDetailsServiceImpl, OrderSecurityService
│   │   ├── service/         # Service interfaces
│   │   │   └── impl/        # Service implementations (@Service, @Transactional)
│   │   ├── util/            # AuditDiffUtil, entity-to-DTO mappers
│   │   └── websocket/       # WebSocketEventPublisher, STOMP message types
│   └── resources/
│       ├── static/          # CSS, JS, images
│       ├── templates/
│       │   ├── customer/    # Customer panel pages (dashboard, menu, reserve, reservations)
│       │   └── ...          # Admin, manager, waiter, chef templates
│       ├── application.properties
│       └── schema.sql       # DDL (IF NOT EXISTS — safe to run on every startup)
└── test/
    ├── java/com/restaurant/rms/
    │   ├── controller/      # @WebMvcTest controller tests
    │   ├── integration/     # @SpringBootTest full-context tests (@Disabled by default)
    │   ├── service/         # @ExtendWith(MockitoExtension) unit tests
    │   └── util/            # TestDataFactory — shared test data builders
    └── resources/
        └── application-test.properties   # H2 in-memory config for test profile
```

---

## Screenshots

| Screen | File |
|---|---|
| Login page | `docs/screenshots/01-login.png` |
| Dashboard | `docs/screenshots/02-dashboard.png` |
| Customer panel | `docs/screenshots/03-customer.png` |
| Order management | `docs/screenshots/04-orders.png` |
| Kitchen Display (KDS) | `docs/screenshots/05-kds.png` |
| Floor plan (table view) | `docs/screenshots/06-floor-plan.png` |
| Inventory management | `docs/screenshots/07-inventory.png` |
| Reports & analytics | `docs/screenshots/08-reports.png` |

---

## Database ER Diagram

The ER diagram is available at `docs/er-diagram.png` (generate from IntelliJ: **Database** tool window → right-click schema → **Diagrams → Show Visualization**).

Key relationships:
- `orders` → `restaurant_tables` (many-to-one)
- `orders` → `users` as waiter (many-to-one)
- `order_items` → `orders` (many-to-one), `menu_items` (many-to-one)
- `menu_item_ingredients` bridges `menu_items` ↔ `inventory_items` (many-to-many with quantity)
- `payments` → `orders` (one-to-one)
- `reservations` → `restaurant_tables` (many-to-one)
- `audit_log` — standalone log table; soft-linked by `entity_type` + `entity_id`

---

## Key Design Decisions

- **Session/cookie auth instead of JWT** — the application is server-rendered with Thymeleaf; a session cookie is simpler to manage, avoids token refresh complexity, and integrates natively with Spring Security's CSRF protection.

- **Customer self-registration** — customers register via `/register` and are automatically assigned the `CUSTOMER` role. This keeps staff accounts admin-controlled while allowing public sign-ups.

- **Soft-delete instead of hard-delete for menu items** — deleting a menu item that appears in historical orders would orphan `order_items`. Soft-delete (`deleted_at` timestamp + `@SQLRestriction`) hides items from the menu while preserving referential integrity and audit history.

- **Inventory deduction at order creation** — deducting stock when the order is placed (not when it's served) provides immediate feedback if stock is insufficient and prevents over-selling.

- **State-machine validation in the service layer** — order status transitions (e.g. PENDING → CONFIRMED → PREPARING) are enforced by a `switch` expression in `OrderServiceImpl.validateTransition()`, keeping the rules in one auditable place.

---

## License

This project is licensed under the **MIT License**.


                                                                                                                                            
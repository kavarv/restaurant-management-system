# Restaurant Management System — Project Setup Guide

> **Goal:** Bootstrap a production-ready Spring Boot 3.2 project in IntelliJ IDEA.
> No business logic yet — just scaffolding.

---

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 17 (Eclipse Temurin recommended) | `java -version` |
| Maven | 3.9+ (or use the wrapper `mvnw`) | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| IntelliJ IDEA | 2023.3+ (Ultimate or Community) | Help → About |
| Git | 2.40+ | `git --version` |

---

## Step 1 — Create the Project in IntelliJ IDEA

### 1.1 Open the New Project wizard

1. Launch IntelliJ IDEA.
2. On the Welcome screen click **New Project** (or **File → New → Project…** if a project is already open).
3. In the left panel select **Spring Boot** (requires the Spring Boot plugin, pre-installed in Ultimate; install from Marketplace in Community).

### 1.2 Fill in the project metadata

| Field | Value |
|-------|-------|
| **Name** | `restaurant-management-system` |
| **Location** | choose your workspace folder |
| **Language** | Java |
| **Type** | Maven |
| **Group** | `com.restaurant` |
| **Artifact** | `rms` |
| **Package name** | `com.restaurant.rms` |
| **Project SDK / Java** | 17 |
| **Packaging** | Jar |
| **Spring Boot** | 3.2.5 (or latest 3.2.x) |

Click **Next**.

### 1.3 Select dependencies

Use the search box to find and tick each dependency:

**Web**
- [x] Spring Web
- [x] Thymeleaf
- [x] WebSocket

**Security**
- [x] Spring Security
- [x] Thymeleaf Extras Spring Security 6  *(search "thymeleaf extras")*

**SQL**
- [x] Spring Data JPA
- [x] MySQL Driver

**I/O**
- [x] Validation

**Ops**
- [x] Spring Boot Actuator
- [x] Spring Boot DevTools

**Developer Tools**
- [x] Lombok

Click **Create**.  IntelliJ downloads the project from `start.spring.io`.

### 1.4 Add dependencies not in the wizard

Open `pom.xml` (already provided in this repo) and verify the following are present — the wizard does not offer them:

```xml
<!-- springdoc OpenAPI / Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- iText 5 PDF generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>

<!-- OpenCSV -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.8</version>
</dependency>
```

After editing `pom.xml`, click the **Maven reload** icon (top-right of the editor) or press `Ctrl+Shift+O` (⌘⇧O on Mac) to download the new jars.

### 1.5 Enable annotation processing (Lombok)

`Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
→ tick **Enable annotation processing** → OK.

---

## Step 2 — Configure Environment Variables

The datasource credentials are injected via environment variables so they are never hard-coded.

> **Which username and password?**
> Use the dedicated app user created by `docs/db-setup.sql` in Step 3 — **not** your MySQL root account.
> `rms_user` / `StrongPass!23` are the example values from that script.
> If you ran the script as-is, use those values here.
> If you chose your own username or password when running the script, substitute them instead.

### Option A — IntelliJ Run Configuration (recommended for local dev)

1. `Run → Edit Configurations…`
2. Select the `RmsApplication` configuration.
3. In **Environment variables** add (replace values if you chose different credentials in Step 3):
   ```
   DB_USERNAME=rms_user
   DB_PASSWORD=StrongPass!23
   ```
4. Click OK.

### Option B — `.env` file with EnvFile plugin

1. Install the **EnvFile** plugin (Settings → Plugins → search "EnvFile").
2. Create `.env` in the project root (already in `.gitignore`):
   ```
   DB_USERNAME=rms_user
   DB_PASSWORD=StrongPass!23
   ```
3. In Run Configuration → EnvFile tab → enable → add `.env`.

### Option C — Shell export (terminal runs)

```bash
export DB_USERNAME=rms_user
export DB_PASSWORD=StrongPass!23
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Step 3 — MySQL Database Setup

### Option A — Run from IntelliJ (easiest on Windows, no terminal needed)

`db-setup.sql` is already open in IntelliJ. Use its built-in Database panel to run it directly:

1. In the **Database** panel on the right, click **"Create data source…"** (or `Alt+Insert`)
2. Choose **MySQL**
3. Fill in:
   - Host: `localhost` | Port: `3306`
   - Database: *(leave blank for now — we're connecting as root to create it)*
   - User: `root` | Password: *(your MySQL root password)*
4. Click **"Download missing driver files"** if prompted, then **Test Connection** → ✅ Successful → **OK**
5. Switch back to `db-setup.sql`. You should now see the root connection in the toolbar at the top of the editor.
6. Click the **▶ Run** button (green play icon at the top of the editor) to execute the entire script.
7. The Output panel at the bottom will confirm each statement ran successfully.

That's it — database and user are now created.

---

### Option B — Run from IntelliJ's built-in terminal

IntelliJ has its own terminal that opens already `cd`'d to the project root, so PATH issues don't apply:

1. **View → Tool Windows → Terminal** (or `Alt+F12`)
2. Start MySQL if not already running:
   ```powershell
   net start MySQL80
   ```
3. Run the script non-interactively using the full path to `mysql.exe` (adjust version if needed):
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < docs\db-setup.sql
   ```
   Enter your root password when prompted.

---

### Option C — System terminal (PowerShell / CMD)

If you prefer a regular terminal, `cd` into the project root first so the file path resolves correctly:

```powershell
# ① Navigate to the project root
cd "C:\Users\kavip\Claude\Projects\DEA web application\restaurant-management-system"

# ② Start MySQL if not running
net start MySQL80

# ③ Run the script (adjust mysql.exe path to match your MySQL version)
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < docs\db-setup.sql
```

> **"mysql is not recognized" even with the full path?**
> Check your actual MySQL install directory in `C:\Program Files\MySQL\` and adjust the version number in the path accordingly.

---

### Verify the setup

After running via any option above, confirm in IntelliJ Database panel (or re-connect as root and run):

```sql
SHOW DATABASES LIKE 'rms_db';
SHOW GRANTS FOR 'rms_user'@'localhost';
```

### Connect the app user in IntelliJ Database panel

Once the database is created, add a second data source for the app user (used by Spring Boot):

1. Database panel → **+** → **Data Source** → **MySQL**
2. Fill in:
   - Host: `localhost` | Port: `3306`
   - Database: `rms_db`
   - User: `rms_user` | Password: `StrongPass!23`
3. Click **Test Connection** → ✅ Successful → **OK**

---

## Step 4 — First Run Verification

### Recommended: run directly from IntelliJ (no terminal needed)

1. Make sure `DB_USERNAME` and `DB_PASSWORD` are set in the run configuration (Step 2, Option A).
2. Click the green **▶ Run** button next to `RmsApplication` in the top toolbar, or press **Shift+F10**.

IntelliJ uses its own bundled Maven — no `mvnw.cmd` or system Maven required.

---

### From the terminal (Windows PowerShell)

> **Note:** `mvnw.cmd` is the Maven Wrapper — a script that downloads Maven automatically so you don't need it installed globally. This project now includes `mvnw.cmd` and `.mvn/wrapper/maven-wrapper.properties` in the project root.
>
> The first run downloads Maven (~10 MB) into your user home directory and may take a minute. Subsequent runs are instant.

Set all three required variables in your PowerShell session, then run:

```powershell
# 1. Point to your JDK (adjust the version folder if yours differs)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"

# 2. Database credentials
$env:DB_USERNAME = "rms_user"
$env:DB_PASSWORD = "StrongPass!23"

# 3. Run
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> **How to find your exact JDK path if the folder name differs:**
> ```powershell
> dir "C:\Program Files\Java\"
> ```
> Use whichever `jdk-XX` folder is listed.

> **Set JAVA_HOME permanently (so you never need to do this again):**
> 1. Search Windows → "Edit the system environment variables" → Environment Variables
> 2. Under *System variables* → New
>    - Name: `JAVA_HOME`
>    - Value: `C:\Program Files\Java\jdk-21.0.11`
> 3. Also add `%JAVA_HOME%\bin` to the `Path` variable
> 4. Click OK, open a **new** PowerShell window, then retry without the `$env:JAVA_HOME` line.

---

Expected output (last few lines):
```
Tomcat started on port(s): 8080 (http) with context path '/'
Started RmsApplication in X.XXX seconds
```

Verify in your browser:
- `http://localhost:8080/actuator/health` → `{"status":"UP"}`
- `http://localhost:8080/swagger-ui.html` → Swagger UI (empty for now)

> **Troubleshooting startup failures:**
>
> | Error message | Cause | Fix |
> |---|---|---|
> | `Port 8080 was already in use` | A previous run (IntelliJ or terminal) is still running | Stop it first — see below |
> | `Access denied for user 'rms_user'@'localhost'` | db-setup.sql has not been run yet | Complete Step 3 to create the MySQL user |
> | `rms_db` schema mismatch / validate failure | No tables exist yet | Complete Step 3; tables will be added in the next development phase |
> | `DB_USERNAME` / `DB_PASSWORD` not set | Env vars missing | Set them in PowerShell or IntelliJ run config (Step 2) |
>
> **To kill a process already using port 8080 (Windows PowerShell):**
> ```powershell
> # Find the PID using port 8080
> netstat -ano | findstr :8080
>
> # Kill it (replace 12345 with the PID shown in the last column above)
> taskkill /PID 12345 /F
> ```
> Or simply stop the IntelliJ run by clicking the red ■ Stop button in the Run panel before running from the terminal.

---

## Step 5 — Git Initialisation

Run these commands inside the project root directory:

```bash
# 1. Initialise the Git repository
git init

# 2. Stage everything (the .gitignore will exclude IDE / build / secrets files)
git add .

# 3. Verify what is staged (confirm no .env, no target/, no .idea/)
git status

# 4. Create the first commit
git commit -m "chore: initial project scaffolding

- Spring Boot 3.2.5 / Java 17 / Maven
- All dependencies: Web, JPA, Security, Thymeleaf, WebSocket,
  Actuator, Lombok, Validation, OpenAPI, iText, OpenCSV
- application.properties base + dev + prod profiles
- Package structure for all domains (no business logic yet)
- MySQL setup script and .gitignore"

# 5. (Optional) Push to a remote repository
git remote add origin https://github.com/<your-org>/restaurant-management-system.git
git branch -M main
git push -u origin main
```

### Commit message convention (follow throughout the project)

Use [Conventional Commits](https://www.conventionalcommits.org/):

| Type | When to use |
|------|-------------|
| `feat:` | new feature |
| `fix:` | bug fix |
| `chore:` | tooling, dependencies, config (no production code) |
| `refactor:` | code restructure without behaviour change |
| `test:` | adding or fixing tests |
| `docs:` | documentation only |
| `perf:` | performance improvement |

Examples:
```
feat(order): add create-order REST endpoint
fix(auth): redirect loop when session expires
chore(deps): bump spring-boot to 3.2.5
test(menu): add unit tests for MenuService
```

---

## Package Structure Reference

```
src/main/java/com/restaurant/rms/
├── RmsApplication.java          ← entry point (@SpringBootApplication)
├── config/                      ← SecurityConfig, WebSocketConfig, OpenApiConfig, DataInitializer
├── controller/
│   ├── auth/                    ← login, logout, registration, password-reset
│   ├── admin/                   ← user/role management (ROLE_ADMIN)
│   ├── waiter/                  ← table selection, order taking (ROLE_WAITER)
│   ├── chef/                    ← KDS view, order status updates (ROLE_CHEF)
│   ├── manager/                 ← reports, menu CRUD (ROLE_MANAGER)
│   └── api/                     ← REST endpoints → JSON responses (/api/v1/**)
├── service/
│   ├── OrderService.java        ← interfaces (one per domain)
│   └── impl/
│       └── OrderServiceImpl.java ← @Service @Transactional implementations
├── repository/                  ← JpaRepository<Entity, ID> interfaces
├── entity/                      ← @Entity JPA classes
├── dto/
│   ├── request/                 ← inbound payloads (@Valid)
│   └── response/                ← outbound payloads (no entity leakage)
├── exception/                   ← ResourceNotFoundException, GlobalExceptionHandler
├── security/                    ← UserDetailsServiceImpl, CustomAuthSuccessHandler
├── websocket/                   ← OrderStatusMessage, WebSocketEventPublisher
├── report/                      ← PdfReportGenerator, CsvExportService
└── util/                        ← stateless helpers (no Spring beans)
```

---

## What's Next (Step 2 of development)

1. **Schema design** — write `docs/schema.sql` with all CREATE TABLE statements
2. **Flyway** — add `org.flywaydb:flyway-mysql` and place migrations under `src/main/resources/db/migration/`
3. **Entities** — implement JPA entity classes in the `entity` package
4. **Security** — implement `SecurityConfig` and `UserDetailsServiceImpl`
5. **First feature** — Menu management CRUD (entity → repository → service → controller → templates)

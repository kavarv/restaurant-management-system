-- ============================================================
-- db-setup.sql  —  MySQL 8 initial database + user setup
-- Run once as root (or a DBA account):
--   mysql -u root -p < docs/db-setup.sql
-- ============================================================


-- 1. Create the database with full Unicode support
--    utf8mb4 supports all emoji and multi-byte characters;
--    utf8mb4_unicode_ci provides correct case-insensitive collation.
CREATE DATABASE IF NOT EXISTS rms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;


-- 2. Create a dedicated application user (no SUPER privilege)
--    Replace 'StrongPass!23' with a real password before running.
--    In production use an environment-specific password from your secrets manager.
CREATE USER IF NOT EXISTS 'rms_user'@'localhost'
    IDENTIFIED BY 'StrongPass!23';


-- 3. Grant only the privileges the application needs:
--    SELECT, INSERT, UPDATE, DELETE  — normal CRUD operations
--    CREATE, ALTER, DROP, INDEX      — needed for Hibernate DDL and Flyway migrations
--    REFERENCES                      — needed to create foreign key constraints
--
--    NOT granted: FILE, SUPER, PROCESS, RELOAD, SHUTDOWN, REPLICATION *
--    This follows the principle of least privilege.
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES
    ON rms_db.*
    TO 'rms_user'@'localhost';


-- 4. Apply the privilege changes immediately
FLUSH PRIVILEGES;


-- 5. Verify the setup (optional — run manually after the script)
-- SHOW GRANTS FOR 'rms_user'@'localhost';
-- SHOW DATABASES LIKE 'rms_db';


-- ============================================================
-- CONNECTING FROM IntelliJ IDEA Database Panel
-- ============================================================
-- 1. View → Tool Windows → Database   (or ⌘5 / Alt+6)
-- 2. Click "+"  →  Data Source  →  MySQL
-- 3. Fill in:
--      Host:     localhost
--      Port:     3306
--      Database: rms_db
--      User:     rms_user
--      Password: StrongPass!23
-- 4. Click "Test Connection" — should show "Successful"
--    (If it fails, ensure MySQL is running: `mysql.server start` on Mac,
--     `net start MySQL80` on Windows, `sudo systemctl start mysql` on Linux)
-- 5. Click OK — the schema tree appears in the Database panel
-- ============================================================

-- ============================================================
-- Restaurant Management System — schema.sql
-- MySQL 8.x  |  Run this once before starting the application
-- ============================================================

CREATE DATABASE IF NOT EXISTS restaurant_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_db;

-- ── users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL,
    email       VARCHAR(100)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL,
    phone       VARCHAR(15),
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── categories ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    display_order INT,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── inventory_items ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_items (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150)    NOT NULL,
    unit           VARCHAR(30)     NOT NULL,
    current_stock  DECIMAL(10,3)   NOT NULL,
    minimum_stock  DECIMAL(10,3)   NOT NULL,
    cost_per_unit  DECIMAL(10,2)   NOT NULL,
    supplier_name  VARCHAR(200),
    created_at     DATETIME(6)     NOT NULL,
    updated_at     DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inventory_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── menu_items ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_items (
    id                        BIGINT          NOT NULL AUTO_INCREMENT,
    name                      VARCHAR(150)    NOT NULL,
    description               TEXT,
    price                     DECIMAL(10,2)   NOT NULL,
    category_id               BIGINT          NOT NULL,
    is_available              TINYINT(1)      NOT NULL DEFAULT 1,
    is_vegetarian             TINYINT(1)      NOT NULL DEFAULT 0,
    is_vegan                  TINYINT(1)      NOT NULL DEFAULT 0,
    is_gluten_free            TINYINT(1)      NOT NULL DEFAULT 0,
    image_url                 VARCHAR(500),
    preparation_time_minutes  INT,
    calories                  INT,
    deleted_at                DATETIME(6),
    created_at                DATETIME(6)     NOT NULL,
    updated_at                DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── menu_item_ingredients ────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_item_ingredients (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    menu_item_id      BIGINT         NOT NULL,
    inventory_item_id BIGINT         NOT NULL,
    quantity          DECIMAL(10,3)  NOT NULL,
    unit_override     VARCHAR(30),
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_menu_ingredient       UNIQUE (menu_item_id, inventory_item_id),
    CONSTRAINT fk_mii_menu_item         FOREIGN KEY (menu_item_id)      REFERENCES menu_items      (id),
    CONSTRAINT fk_mii_inventory_item    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── restaurant_tables ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    table_number         INT          NOT NULL,
    capacity             INT          NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    location_description VARCHAR(100),
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tables_number UNIQUE (table_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── orders ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    table_id         BIGINT,
    waiter_id        BIGINT          NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    order_type       VARCHAR(20)     NOT NULL,
    notes            TEXT,
    total_amount     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    delivery_address TEXT,
    created_at       DATETIME(6)     NOT NULL,
    updated_at       DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_table  FOREIGN KEY (table_id)  REFERENCES restaurant_tables (id),
    CONSTRAINT fk_orders_waiter FOREIGN KEY (waiter_id) REFERENCES users             (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── order_items ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    order_id      BIGINT         NOT NULL,
    menu_item_id  BIGINT         NOT NULL,
    quantity      INT            NOT NULL,
    unit_price    DECIMAL(10,2)  NOT NULL,
    special_notes TEXT,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order     FOREIGN KEY (order_id)     REFERENCES orders     (id),
    CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── payments ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    order_id         BIGINT         NOT NULL,
    amount           DECIMAL(10,2)  NOT NULL,
    payment_method   VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    transaction_id   VARCHAR(200),
    paid_at          DATETIME(6),
    gateway_response TEXT,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_order  UNIQUE (order_id),
    CONSTRAINT fk_payments_order  FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── reservations ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reservations (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id       BIGINT       NOT NULL,
    table_id          BIGINT       NOT NULL,
    reserved_date     DATETIME(6)  NOT NULL,
    party_size        INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes             TEXT,
    confirmation_code VARCHAR(20),
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES users             (id),
    CONSTRAINT fk_reservations_table    FOREIGN KEY (table_id)    REFERENCES restaurant_tables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── audit_logs ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      BIGINT       NOT NULL,
    action         VARCHAR(20)  NOT NULL,
    changed_by_id  BIGINT,
    old_values     TEXT,
    new_values     TEXT,
    ip_address     VARCHAR(45),
    changed_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity     (entity_type, entity_id),
    INDEX idx_audit_changed_by (changed_by_id),
    CONSTRAINT fk_audit_changed_by FOREIGN KEY (changed_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── seed: users are seeded by DataInitializer (Spring ApplicationRunner) ──────
-- Removed hard-coded INSERT here because DataInitializer runs after schema.sql
-- and already creates all staff accounts with BCrypt-hashed passwords.
-- Keeping a raw INSERT IGNORE here would race with DataInitializer and produce
-- a different password hash, causing login failures.

-- ── Customer panel migration: anonymous reservations ─────────
-- Make customer_id nullable so walk-in / public bookings don't need an account
-- NOTE: column additions (customer_name, customer_email, customer_phone) are
-- handled by Hibernate ddl-auto=update from the entity definition.

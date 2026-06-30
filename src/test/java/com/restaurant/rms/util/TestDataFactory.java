package com.restaurant.rms.util;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.LoginRequest;
import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.request.OrderItemRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.entity.*;
import com.restaurant.rms.entity.enums.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Static factory for building entity and DTO instances in tests.
 * All builders use sensible defaults — callers only override what they care about.
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    // ── Entities ────────────────────────────────────────────────────────────

    public static Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    public static MenuItem menuItem(Long id, String name, BigDecimal price, Category category) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        m.setDescription("Test description for " + name);
        m.setPrice(price);
        m.setCategory(category);
        m.setIsAvailable(true);
        m.setIsVegetarian(false);
        m.setIsVegan(false);
        m.setIsGlutenFree(false);
        m.setPreparationTimeMinutes(15);
        m.setCalories(500);
        return m;
    }

    public static MenuItem menuItem(Long id, String name) {
        return menuItem(id, name, new BigDecimal("9.99"), category(1L, "Mains"));
    }

    public static InventoryItem inventoryItem(Long id, String name, BigDecimal stock, BigDecimal minStock) {
        InventoryItem i = new InventoryItem();
        i.setId(id);
        i.setName(name);
        i.setUnit("kg");
        i.setCurrentStock(stock);
        i.setMinimumStock(minStock);
        i.setCostPerUnit(new BigDecimal("2.50"));
        i.setSupplierName("Test Supplier");
        return i;
    }

    public static InventoryItem inventoryItem(Long id, String name) {
        return inventoryItem(id, name, new BigDecimal("50.000"), new BigDecimal("5.000"));
    }

    public static User waiter(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setPassword("$2a$10$hashed");
        u.setRole(Role.WAITER);
        u.setIsActive(true);
        return u;
    }

    public static User admin(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("admin");
        u.setEmail("admin@test.com");
        u.setPassword("$2a$10$hashed");
        u.setRole(Role.ADMIN);
        u.setIsActive(true);
        return u;
    }

    public static RestaurantTable table(Long id, int tableNumber, TableStatus status) {
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        t.setTableNumber(tableNumber);
        t.setCapacity(4);
        t.setStatus(status);
        return t;
    }

    public static RestaurantTable availableTable(Long id) {
        return table(id, (int)(long)id, TableStatus.AVAILABLE);
    }

    public static Order order(Long id, RestaurantTable table, User waiter, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setTable(table);
        o.setWaiter(waiter);
        o.setStatus(status);
        o.setOrderType(OrderType.DINE_IN);
        o.setTotalAmount(new BigDecimal("22.00"));
        o.setItems(new ArrayList<>());
        return o;
    }

    public static OrderItem orderItem(Long id, Order order, MenuItem menuItem, int qty) {
        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setOrder(order);
        oi.setMenuItem(menuItem);
        oi.setQuantity(qty);
        oi.setUnitPrice(menuItem.getPrice());
        oi.setStatus(OrderItemStatus.PENDING);
        return oi;
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    public static MenuItemRequest menuItemRequest(String name, BigDecimal price, Long categoryId) {
        MenuItemRequest r = new MenuItemRequest();
        r.setName(name);
        r.setDescription("A delicious " + name);
        r.setPrice(price);
        r.setCategoryId(categoryId);
        r.setIsAvailable(true);
        r.setIsVegetarian(false);
        r.setPreparationTimeMinutes(20);
        r.setCalories(450);
        return r;
    }

    public static MenuItemRequest menuItemRequest() {
        return menuItemRequest("Grilled Chicken", new BigDecimal("14.99"), 1L);
    }

    public static CreateOrderRequest createOrderRequest(Long tableId, Long... menuItemIds) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setTableId(tableId);
        r.setOrderType(OrderType.DINE_IN);
        r.setNotes("Test order");
        r.setWaiterId(5L); // set explicitly so controller does not cast @AuthenticationPrincipal
        List<OrderItemRequest> items = new ArrayList<>();
        for (Long menuItemId : menuItemIds) {
            OrderItemRequest item = new OrderItemRequest();
            item.setMenuItemId(menuItemId);
            item.setQuantity(2);
            items.add(item);
        }
        r.setItems(items);
        return r;
    }

    public static OrderStatusUpdateRequest statusUpdate(OrderStatus status) {
        OrderStatusUpdateRequest r = new OrderStatusUpdateRequest();
        r.setStatus(status);
        return r;
    }

    public static LoginRequest loginRequest(String
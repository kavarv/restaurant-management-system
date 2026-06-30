package com.restaurant.rms.integration;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderItemRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.InventoryItemResponse;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import com.restaurant.rms.service.InventoryService;
import com.restaurant.rms.service.OrderService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying inventory is correctly deducted and restored
 * across the order lifecycle.
 *
 * <p>Requires test database profile — see {@link OrderIntegrationTest} for setup.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("Requires test database profile — remove @Disabled to run locally")
@DisplayName("Inventory stock deduction integration tests")
class InventoryIntegrationTest {

    @Autowired InventoryService inventoryService;
    @Autowired OrderService     orderService;

    @Test
    @DisplayName("stockDeductionOnOrder: stock reduces on order create, restores on cancel")
    void stockDeductionOnOrder() {
        // Record stock before order (assumes ingredient linked to menuItem id=1)
        InventoryItemResponse before = inventoryService.findById(1L);
        BigDecimal stockBefore = before.getCurrentStock();

        // Create order with 1 unit of menu item 1
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(1L);
        item.setQuantity(1);

        CreateOrderRequest createReq = new CreateOrderRequest();
        createReq.setTableId(1L);
        createReq.setOrderType(OrderType.DINE_IN);
        createReq.setItems(List.of(item));

        OrderResponse order = orderService.createOrder(createReq, 1L);
        Long orderId = order.getId();

        // Verify stock reduced
        InventoryItemResponse afterOrder = inventoryService.findById(1L);
        assertThat(afterOrder.getCurrentStock()).isLessThan(stockBefore);

        // Confirm order (moves it past PENDING so cancel triggers inventory restore)
        OrderStatusUpdateRequest confirm = new OrderStatusUpdateRequest();
        confirm.setStatus(OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(orderId, confirm);

        // Cancel → inventory should be restored
        orderService.cancelOrder(orderId, "Integration test cancel");

        InventoryItemResponse afterCancel = inventoryService.findById(1L);
        assertThat
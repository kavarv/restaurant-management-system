package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.*;
import com.restaurant.rms.entity.enums.*;
import com.restaurant.rms.exception.InsufficientStockException;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.*;
import com.restaurant.rms.service.impl.OrderServiceImpl;
import com.restaurant.rms.util.TestDataFactory;
import com.restaurant.rms.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService unit tests")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock RestaurantTableRepository tableRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryItemRepository inventoryRepository;
    @Mock MenuItemIngredientRepository ingredientRepository;
    @Mock AuditService auditService;
    @Mock WebSocketEventPublisher wsPublisher;

    @InjectMocks OrderServiceImpl service;

    private RestaurantTable table;
    private User waiter;
    private MenuItem menuItem;
    private InventoryItem stockItem;
    private MenuItemIngredient ingredient;

    @BeforeEach
    void setUp() {
        table    = TestDataFactory.availableTable(1L);
        waiter   = TestDataFactory.waiter(5L, "waiter1");
        menuItem = TestDataFactory.menuItem(20L, "Burger");
        stockItem = TestDataFactory.inventoryItem(30L, "Beef Patty",
                new BigDecimal("100.000"), new BigDecimal("5.000"));

        ingredient = new MenuItemIngredient();
        ingredient.setId(1L);
        ingredient.setMenuItem(menuItem);
        ingredient.setInventoryItem(stockItem);
        ingredient.setQuantity(new BigDecimal("0.200")); // 200g per burger
    }

    // ── createOrder ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder_success: inventory deducted, table OCCUPIED, WebSocket published")
    void testCreateOrder_success() {
        CreateOrderRequest request = TestDataFactory.createOrderRequest(1L, 20L);

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(userRepository.findById(5L)).thenReturn(Optional.of(waiter));
        when(menuItemRepository.findById(20L)).thenReturn(Optional.of(menuItem));
        when(ingredientRepository.findByMenuItemId(20L)).thenReturn(List.of(ingredient));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order savedOrder = TestDataFactory.order(100L, table, waiter, OrderStatus.PENDING);
        savedOrder.setItems(new ArrayList<>());

        // First save = order shell; second save = after totals
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem oi = inv.getArgument(0);
            oi.setId(999L);
            return oi;
        });
        when(orderItemRepository.findByOrder(any())).thenReturn(new ArrayList<>());

        OrderResponse response = service.createOrder(request, 5L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        // 2 burgers × 0.200 kg = 0.400 deducted from 100.000 → 99.600
        assertThat(stockItem.getCurrentStock()).isEqualByComparingTo("99.600");
        verify(wsPublisher).publishNewOrder(any(Order.class));
        verify(auditService).log(eq("Order"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createOrder_insufficientStock: throws InsufficientStockException")
    void testCreateOrder_insufficientStock() {
        stockItem.setCurrentStock(new BigDecimal("0.100")); // only 100g, need 400g for 2
        CreateOrderRequest request = TestDataFactory.createOrderRequest(1L, 20L);

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(userRepository.findById(5L)).thenReturn(Optional.of(waiter));
        when(menuItemRepository.findById(20L)).thenReturn(Optional.of(menuItem));
        when(ingredientRepository.findByMenuItemId(20L)).thenReturn(List.of(ingredient));

        Order shell = TestDataFactory.order(100L, table, waiter, OrderStatus.PENDING);
        shell.setItems(new ArrayList<>());
        when(orderRepository.save(any())).thenReturn(shell);

        assertThatThrownBy(() -> service.createOrder(request, 5L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Beef Patty");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder_tableNotAvailable: MAINTENANCE table throws InvalidOperationException")
    void testCreateOrder_tableNotAvailable() {
        table.setStatus(TableStatus.MAINTENANCE);
        CreateOrderRequest request = TestDataFactory.createOrderRequest(1L, 20L);

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> service.createOrder(request, 5L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    // ── updateOrderStatus ─────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus_validTransition: PENDING → CONFIRMED publishes WebSocket event")
    void testUpdateOrderStatus_validTransition() {
        Order order = TestDataFactory.order(100L, table, waiter, OrderStatus.PENDING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrder(any())).thenReturn(new ArrayList<>());

        OrderStatusUpdateRequest req = TestDataFactory.statusUpdate(OrderStatus.CONFIRMED);
        OrderResponse response = service.updateOrderStatus(100L, req);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(wsPublisher).publishOrderStatusChange(any(Order.class));
    }

    @Test
    @DisplayName("updateOrderStatus_invalidTransition: SERVED → PENDING throws InvalidOperationException")
    void testUpdateOrderStatus_invalidTransition() {
        Order order = TestDataFactory.order(100L, table, waiter, OrderStatus.SERVED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderStatusUpdateRequest req = TestDataFactory.statusUpdate(OrderStatus.PENDING);

        assertThatThrownBy(() -> service.updateOrderStatus(100L, req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("illegal transition");
    }

    @Test
    @DisplayName("cancelOrder_afterConfirmed_restoresInventory: stock incremented back on cancel")
    void testCancelOrder_afterConfirmed_restoresInventory() {
        Order order = TestDataFactory.order(100L, table, waiter, OrderStatus.CONFIRMED);
        order.setItems(new ArrayList<>());

        OrderItem oi = TestDataFactory.orderItem(1L, order, menuItem, 2);
        List<OrderItem> items = List.of(oi);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder(order)).thenReturn(items);
        when(ingredientRepository.findByMenuItemId(20L)).thenReturn(List.of(ingredient));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal stockBefore = stockItem.getCurrentStock(); // 100.000

        OrderStatusUpdateRequest req = TestDataFactory.statusUpdate(OrderStatus.CANCELLED);
        service.updateOrderStatus(100L, req);

        // 2 × 0.200 = 0.400 restored
        assertThat(stockItem.getCurrentStock())
                .isEqualByComparingTo(stockBefore.add(new BigDecimal("0.400")));
    }
}

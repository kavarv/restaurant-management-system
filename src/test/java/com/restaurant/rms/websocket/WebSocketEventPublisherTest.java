package com.restaurant.rms.websocket;

import com.restaurant.rms.entity.*;
import com.restaurant.rms.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebSocketEventPublisher}.
 *
 * <p>These tests verify that the publisher:
 * <ol>
 *   <li>Sends to the expected STOMP destinations with correctly typed payloads.</li>
 *   <li>Never propagates exceptions — a broken broker must not affect the caller.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class WebSocketEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketEventPublisher publisher;

    private Order order;
    private RestaurantTable table;
    private User waiter;

    @BeforeEach
    void setUp() {
        waiter = new User();
        waiter.setId(1L);
        waiter.setUsername("alice");

        table = new RestaurantTable();
        table.setId(10L);
        table.setTableNumber(5);
        table.setCapacity(4);
        table.setStatus(TableStatus.OCCUPIED);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(100L);
        menuItem.setName("Margherita Pizza");
        menuItem.setPrice(new BigDecimal("12.50"));

        OrderItem item = new OrderItem();
        item.setId(200L);
        item.setMenuItem(menuItem);
        item.setQuantity(2);
        item.setStatus(OrderItemStatus.PENDING);

        order = new Order();
        order.setId(42L);
        order.setTable(table);
        order.setWaiter(waiter);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderType(OrderType.DINE_IN);
        order.setTotalAmount(new BigDecimal("25.00"));
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(List.of(item));
    }

    // ── publishNewOrder ───────────────────────────────────────────────────────

    @Test
    void publishNewOrder_sendsKitchenDisplayMessageToKitchenTopic() {
        publisher.publishNewOrder(order);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/kitchen"),
                (Object) argThat(payload -> payload instanceof KitchenDisplayMessage kdm
                        && kdm.getOrderId().equals(42L)
                        && kdm.getTableNumber() == 5
                        && kdm.getItems().size() == 1
                        && "Margherita Pizza".equals(kdm.getItems().get(0).getName())));
    }

    @Test
    void publishNewOrder_setsHighPriorityWhenOrderIsOld() {
        // Simulate an order placed 15 minutes ago
        order.setCreatedAt(LocalDateTime.now().minusMinutes(15));

        publisher.publishNewOrder(order);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/kitchen"),
                (Object) argThat(payload -> payload instanceof KitchenDisplayMessage kdm
                        && "HIGH".equals(kdm.getPriority())));
    }

    @Test
    void publishNewOrder_doesNotThrowWhenBrokerFails() {
        doThrow(new RuntimeException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Must swallow the exception — no exception should propagate
        assertThatNoException().isThrownBy(() -> publisher.publishNewOrder(order));
    }

    // ── publishOrderStatusChange ──────────────────────────────────────────────

    @Test
    void publishOrderStatusChange_broadcastsToThreeTopics() {
        order.setStatus(OrderStatus.PREPARING);

        publisher.publishOrderStatusChange(order);

        // Should send to per-order topic, kitchen, and floor
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/42"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/kitchen"),   any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/floor"),     any(Object.class));
    }

    @Test
    void publishOrderStatusChange_payloadContainsCorrectStatus() {
        order.setStatus(OrderStatus.READY);

        publisher.publishOrderStatusChange(order);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/orders/42"),
                (Object) argThat(payload -> payload instanceof OrderUpdateMessage msg
                        && msg.getStatus() == OrderStatus.READY
                        && msg.getOrderId().equals(42L)
                        && msg.getTableNumber() == 5));
    }

    @Test
    void publishOrderStatusChange_doesNotThrowWhenBrokerFails() {
        doThrow(new RuntimeException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() -> publisher.publishOrderStatusChange(order));
    }

    // ── publishTableStatusUpdate ──────────────────────────────────────────────

    @Test
    void publishTableStatusUpdate_sendsTableStatusMessageToFloorTopic() {
        publisher.publishTableStatusUpdate(table);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/floor"),
                (Object) argThat(payload -> payload instanceof TableStatusMessage msg
                        && msg.getTableId().equals(10L)
                        && msg.getTableNumber() == 5
                        && msg.getStatus() == TableStatus.OCCUPIED));
    }

    @Test
    void publishTableStatusUpdate_doesNotThrowWhenBrokerFails() {
        doThrow(new RuntimeException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatNoException().isThrownBy(() -> publisher.publishTableStatusUpdate(table));
    }

    // ── Takeaway order (null table) ───────────────────────────────────────────

    @Test
    void publishNewOrder_handlesNullTableGracefully() {
        order.setTable(null); // takeaway order

        publisher.publishNewOrder(order);

        verify(messagingTemplate).convertAndSend(
               
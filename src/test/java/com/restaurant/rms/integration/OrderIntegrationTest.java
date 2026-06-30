package com.restaurant.rms.integration;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderItemRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.request.PaymentRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import com.restaurant.rms.entity.enums.PaymentMethod;
import com.restaurant.rms.service.OrderService;
import com.restaurant.rms.service.PaymentService;
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
 * Full order lifecycle integration test.
 *
 * <p>Requires a running database (H2 in-memory with test profile, or
 * Testcontainers MySQL). Annotated with {@code @Disabled} so the CI pipeline
 * runs only when the database is available — remove {@code @Disabled} to run locally.</p>
 *
 * <p>Test profile ({@code application-test.properties}) should configure:
 * <pre>
 *   spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
 *   spring.jpa.hibernate.ddl-auto=create-drop
 *   spring.sql.init.mode=never
 * </pre>
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // rolls back after each test — keeps tests independent
@Disabled("Requires test database profile — remove @Disabled to run locally with H2 or Testcontainers")
@DisplayName("Order lifecycle integration tests")
class OrderIntegrationTest {

    @Autowired OrderService   orderService;
    @Autowired PaymentService paymentService;

    @Test
    @DisplayName("fullOrderLifecycle: PENDING→CONFIRMED→PREPARING→READY→SERVED→COMPLETED→PAID")
    void fullOrderLifecycle() {
        // 1. Arrange — build a minimal DINE_IN order (assumes seeded data from DataInitializer)
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setMenuItemId(1L);
        item1.setQuantity(1);

        CreateOrderRequest createReq = new CreateOrderRequest();
        createReq.setTableId(1L);
        createReq.setOrderType(OrderType.DINE_IN);
        createReq.setItems(List.of(item1));

        // 2. Create — PENDING
        OrderResponse order = orderService.createOrder(createReq, 1L); // waiter id=1
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        Long orderId = order.getId();

        // 3. PENDING → CONFIRMED
        order = transition(orderId, OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // 4. CONFIRMED → PREPARING
        order = transition(orderId, OrderStatus.PREPARING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        // 5. PREPARING → READY
        order = transition(orderId, OrderStatus.READY);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);

        // 6. READY → SERVED
        order = transition(orderId, OrderStatus.SERVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVED);

        // 7. SERVED → COMPLETED
        order = transition(orderId, OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // 8. Process payment
        PaymentRequest payReq = new PaymentRequest();
        payReq.setOrderId(orderId);
        payReq.setPaymentMethod(PaymentMethod.CASH);
        payReq.setAmount(order.getTotalAmount().add(new BigDecimal("5.00"))); // slight tip

        var payment = paymentService.processPayment(payReq);
        assertThat(payment).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
    }

    private OrderResponse transition(Long orderId, OrderStatus to) {
        OrderStatusUpdateRequest req = new OrderStatusUpdateReque
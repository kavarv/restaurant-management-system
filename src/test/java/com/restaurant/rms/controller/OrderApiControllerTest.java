package com.restaurant.rms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.controller.api.OrderApiController;
import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import com.restaurant.rms.exception.GlobalExceptionHandler;
import com.restaurant.rms.exception.InsufficientStockException;
import com.restaurant.rms.security.OrderSecurityService;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.OrderService;
import com.restaurant.rms.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any; // explicit wins over Hamcrest wildcard
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderApiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("OrderApiController web-layer tests")
class OrderApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  OrderService orderService;
    @MockBean  OrderSecurityService orderSecurityService;  // bean used in SpEL

    // ── POST /api/v1/orders ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "waiter1", roles = {"WAITER"})
    @DisplayName("POST /api/v1/orders valid request returns 201")
    void testCreateOrder_success() throws Exception {
        CreateOrderRequest request = TestDataFactory.createOrderRequest(1L, 10L, 11L);

        OrderResponse response = OrderResponse.builder()
                .id(100L).status(OrderStatus.PENDING)
                .orderType(OrderType.DINE_IN).tableId(1L)
                .totalAmount(new BigDecimal("22.00")).items(List.of())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/orders/100")))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @WithMockUser(username = "waiter1", roles = {"WAITER"})
    @DisplayName("POST /api/v1/orders with insufficient stock returns 422")
    void testCreateOrder_insufficientStock_returns422() throws Exception {
        CreateOrderRequest request = TestDataFactory.createOrderRequest(1L, 10L);

        when(orderService.createOrder(any(), anyLong()))
                .thenThrow(new InsufficientStockException("Beef Patty",
                        new BigDecimal("0.400"), new BigDecimal("0.100")));

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Beef Patty")));
    }

    // ── PATCH /api/v1/orders/{id}/status ──────────────────────────────────

    @Test
    @WithMockUser(username = "manager1", roles = {"MANAGER"})
    @DisplayName("PATCH /api/v1/orders/{id}/status valid transition returns 200")
    void testUpdateOrderStatus_validTransition() throws Exception {
        OrderStatusUpdateRequest req = TestDataFactory.statusUpdate(OrderStatus.CONFIRMED);

        OrderResponse response = OrderResponse.builder()
                .id(100L).status(OrderStatus.CONFIRMED)
                .orderType(OrderType.DINE_IN).items(List.of())
                .totalAmount(new BigDecimal("22.00"))
                .build();

        when(orderSecurityService.canUpdateStatus(eq(100L), any())).thenReturn(true);
        when(orderService.updateOrderStatus(eq(100L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/100/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    @WithMockUser(username = "chef1", roles = {"CHEF"})
    @DisplayName("PATCH /api/v1/orders/{id}/status chef denied returns 403")
    void testUpdateOrderStatus_chefDenied_returns403() throws Exception {
        OrderStatusUpdateRequest req = TestDataFactory.statusUpdate(OrderStatus.SERVED);

        // OrderSecurityService denies this chef for this order
        when(orderSecurityService.canUpdateStatus(eq(100L), any())).thenReturn(false);

        mockMvc.perform(patch("/api/v1/orders/100/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/v1/orders/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(username = "manager1", roles = {"MANAGER"})
    @DisplayName("DELETE /api/v1/orders/{id} returns 204")
    void testCancelOrder_returns204() throws Exception {
        doNothing().when(orderService).cancelOrder(100L, null);

        mockMvc.perform(delete("/api/v1/ord
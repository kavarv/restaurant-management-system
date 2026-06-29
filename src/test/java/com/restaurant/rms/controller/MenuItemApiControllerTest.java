package com.restaurant.rms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.controller.api.MenuItemApiController;
import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.exception.GlobalExceptionHandler;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.service.MenuItemService;
import com.restaurant.rms.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

@WebMvcTest(MenuItemApiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MenuItemApiController web-layer tests")
class MenuItemApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  MenuItemService menuItemService;

    // ── GET /api/v1/menu ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "waiter1", roles = {"WAITER"})
    @DisplayName("GET /api/v1/menu returns 200 with page structure")
    void testGetAllMenuItems_returnsPage() throws Exception {
        MenuItemResponse item = MenuItemResponse.builder()
                .id(1L).name("Burger").price(new BigDecimal("12.99"))
                .categoryId(1L).categoryName("Mains").isAvailable(true)
                .build();

        PagedResponse<MenuItemResponse> page = PagedResponse.from(
                new PageImpl<>(List.of(item)), r -> r);

        when(menuItemService.findAll(any(Pageable.class), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/menu").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Burger")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    // ── POST /api/v1/menu ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/menu with valid request returns 201 + Location header")
    void testCreateMenuItem_validRequest_returns201() throws Exception {
        MenuItemRequest request = TestDataFactory.menuItemRequest("Cheese Burger", new BigDecimal("13.99"), 1L);

        MenuItemResponse created = MenuItemResponse.builder()
                .id(42L).name("Cheese Burger").price(new BigDecimal("13.99"))
                .categoryId(1L).categoryName("Mains").isAvailable(true)
                .build();

        when(menuItemService.create(any(MenuItemRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/menu")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/menu/42")))
                .andExpect(jsonPath("$.id", is(42)))
                .andExpect(jsonPath("$.name", is("Cheese Burger")))
                .andExpect(jsonPath("$.price", is(13.99)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/menu with missing name returns 400 with fieldErrors")
    void testCreateMenuItem_invalidRequest_returns400() throws Exception {
        MenuItemRequest bad = new MenuItemRequest();
        // name is blank, price null, categoryId null — all violate @NotBlank/@NotNull

        mockMvc.perform(post("/api/v1/menu")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /api/v1/menu without auth returns 401")
    void testCreateMenuItem_unauthorized_returns401() throws Exception {
        MenuItemRequest request = TestDataFactory.menuItemRequest();

        mockMvc.perform(post("/api/v1/menu")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "waiter1", roles = {"WAITER"})
    @DisplayName("POST /api/v1/menu as WAITER returns 403")
    void testCreateMenuItem_forbidden_returns403() throws Exception {
        MenuItemRequest request = TestDataFactory.menuItemRequest();

        mockMvc.perform(post("/api/v1/menu")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/v1/menu/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/menu/{id} as ADMIN returns 204")
    void testDeleteMenuItem_success_returns204() throws Exception {
        doNothing().when(menuItemService).delete(42L);

        mockMvc.perform(delete("/api/v1/menu/42").with(csrf()))
                .andExpect(status().isNoContent());

        verify(menuItemService).delete(42L);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/menu/{id} when not found returns 404")
    void testGetById_notFound_returns404() throws Exception {
        when(menuItemService.findById(999L))
                .thenThrow(new ResourceNotFoundException("MenuItem", "id", 999L));

        mockMvc.perform(get("/api/v1/menu/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("MenuItem")));
    }
}

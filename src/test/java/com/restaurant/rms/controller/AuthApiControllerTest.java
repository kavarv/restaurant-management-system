package com.restaurant.rms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.controller.api.AuthApiController;
import com.restaurant.rms.dto.request.LoginRequest;
import com.restaurant.rms.dto.response.AuthResponse;
import com.restaurant.rms.exception.GlobalExceptionHandler;
import com.restaurant.rms.service.UserService;
import com.restaurant.rms.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthApiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthApiController web-layer tests")
class AuthApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  UserService userService;
    @MockBean  AuthenticationManager authenticationManager;

    // ── POST /api/v1/auth/login ───────────────────────────────────────────

    @Test
    @DisplayName("POST /login with valid credentials returns 200 and session info")
    void testLogin_validCredentials() throws Exception {
        LoginRequest request = TestDataFactory.loginRequest("admin", "Admin@123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.tokenType", is("Session")));
    }

    @Test
    @DisplayName("POST /login with wrong password returns 401")
    void testLogin_invalidCredentials_returns401() throws Exception {
        LoginRequest request = TestDataFactory.loginRequest("admin", "wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login with blank username returns 400")
    void testLogin_missingUsername_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("Admin@123");
        // username is blank

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────────────

    @Test
    @DisplayName("POST /logout returns 204")
    void testLogout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/v1/auth/csrf ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /csrf returns token map")
    void testGetCsrfToken_returnsMap() throws Exception {

package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.LoginRequest;
import com.restaurant.rms.dto.request.RegisterRequest;
import com.restaurant.rms.dto.response.AuthResponse;
import com.restaurant.rms.dto.response.UserResponse;
import com.restaurant.rms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "auth", description = "Authentication — login, logout, registration, CSRF token")
public class AuthApiController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @SecurityRequirements   // no auth required for registration
    @Operation(
        summary     = "Register a new user",
        description = "Creates a user account. Only ADMIN may assign roles other than WAITER.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Registration payload", required = true,
            content     = @Content(schema = @Schema(implementation = RegisterRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or username taken",
                     content = @Content)
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.getId())).body(created);
    }

    @PostMapping("/login")
    @SecurityRequirements   // no auth required
    @Operation(
        summary     = "Login",
        description = "Authenticates credentials and establishes a session. " +
                      "The response includes `Set-Cookie: JSESSIONID=...` — " +
                      "include this cookie on all subsequent requests.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credentials", required = true,
            content     = @Content(schema = @Schema(implementation = LoginRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful; JSESSIONID cookie set",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing credentials",      content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid username/password", content = @Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        httpRequest.getSession(true);
        return ResponseEntity.ok(AuthResponse.builder()
                .username(auth.getName())
                .tokenType("Session")
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidates the current session and clears the security context.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",       content = @Content)
    })
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) new SecurityContextLogoutHandler().logout(request, response, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    @SecurityRequirements   // must be callable without prior auth
    @Operation(
        summary     = "Get CSRF token",
        description = "Returns the current CSRF token. Call this before any mutating request " +
                      "and pass the `token` value as the `X-CSRF-TOKEN` request header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSRF token returned",
                     content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return ResponseEntity.ok(Map.of("message", "CSRF protection is not active for this request"));
        }
        return ResponseEntity.ok(Map.of(
            "token",      csrfToken.getToken(),
        
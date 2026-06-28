package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.ReservationRequest;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.dto.response.ReservationResponse;
import com.restaurant.rms.entity.enums.ReservationStatus;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "reservations", description = "Reservation management — booking, confirmation, cancellation")
public class ReservationApiController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation", description = "Books a table. Status starts as PENDING.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservation created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long callerId = principal != null ? principal.getId() : null;
        ReservationResponse created = reservationService.create(request, callerId);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + created.getId())).body(created);
    }

    @GetMapping("/my")
    @Operation(summary = "My reservations", description = "Returns the authenticated customer's own reservations, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservations returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ResponseEntity<PagedResponse<ReservationResponse>> myReservations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                reservationService.findByCustomer(principal.getId(), PageRequest.of(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List reservations", description = "Paginated list with optional date and status filters.")
    public ResponseEntity<PagedResponse<ReservationResponse>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by reservation date (ISO-8601)", example = "2025-12-25")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Filter by status", example = "PENDING")
            @RequestParam(required = false) ReservationStatus status) {
        return ResponseEntity.ok(reservationService.findAll(PageRequest.of(page, size), date, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get reservation by ID")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Confirm a reservation", description = "Transitions PENDING → CONFIRMED.")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Delete / hard-cancel a reservation")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}

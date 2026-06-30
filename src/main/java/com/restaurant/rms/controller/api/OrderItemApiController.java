package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.OrderItemStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderItemResponse;
import com.restaurant.rms.service.OrderItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order-items")
@RequiredArgsConstructor
public class OrderItemApiController {

    private final OrderItemService orderItemService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CHEF','WAITER')")
    public ResponseEntity<OrderItemResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderItemStatusUpdateRequest request) {
        return ResponseEntity.ok(orderItemService.updateItemStatus(
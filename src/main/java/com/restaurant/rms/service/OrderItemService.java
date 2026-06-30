package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.OrderItemStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderItemResponse;

import java.util.List;

/**
 * Manages individual order item lifecycle — primarily used by kitchen staff.
 */
public interface OrderItemService {

    /**
     * Updates the preparation status of a single order item.
     * Used by the Kitchen Display System (KDS) as a chef updates dish progress.
     *
     * @param orderItemId the order item ID
     * @param request     new status
     * @return updated order item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if the transition is invalid
     */
    OrderItemResponse updateItemStatus(Long orderItemId, OrderItemStatusUpdateRequest request);

    /**
     * Returns all items belonging to a specific order.
     *
     * @param orderId order ID
     * @return list of order item DTOs
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if order not found
     */
    List<OrderItemResponse
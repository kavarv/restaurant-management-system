package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderItemRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Core order management service — the most complex service in the system.
 * Orchestrates inventory deduction, table status, WebSocket events, and audit logging.
 */
public interface OrderService {

    /**
     * Creates a new order:
     * <ol>
     *   <li>Validates table availability</li>
     *   <li>Verifies each menu item is active and available</li>
     *   <li>Snapshots unit prices into order items</li>
     *   <li>Deducts inventory for each ingredient</li>
     *   <li>Sets the table to OCCUPIED</li>
     *   <li>Calculates subtotal + 10% tax = total</li>
     *   <li>Publishes a WebSocket new-order event</li>
     *   <li>Writes an audit log entry</li>
     * </ol>
     *
     * @param request    order payload (table, items, type, notes)
     * @param waiterId   ID of the authenticated waiter creating the order
     * @return created order DTO with all items
     * @throws com.restaurant.rms.exception.ResourceNotFoundException  if table or any menu item not found
     * @throws com.restaurant.rms.exception.InvalidOperationException  if table status is RESERVED or MAINTENANCE
     * @throws com.restaurant.rms.exception.InsufficientStockException if any ingredient is out of stock
     */
    OrderResponse createOrder(CreateOrderRequest request, Long waiterId);

    /**
     * Transitions an order through its status lifecycle:
     * PENDING → CONFIRMED → PREPARING → READY → SERVED → COMPLETED
     * Any status → CANCELLED (MANAGER/ADMIN only; restores inventory if after CONFIRMED).
     *
     * @param id      order ID
     * @param request new status + optional reason
     * @return updated order DTO
     * @throws com.restaurant.rms.exception.InvalidOperationException if the transition is illegal
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if order not found
     */
    OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request);

    /**
     * Adds a new item to an existing open order.
     * Only allowed while order is PENDING or CONFIRMED.
     * Recalculates order total and deducts inventory.
     *
     * @param orderId order ID
     * @param item    item to add
     * @return updated order DTO
     * @throws com.restaurant.rms.exception.InvalidOperationException if order is not in an editable state
     */
    OrderResponse addItemToOrder(Long orderId, OrderItemRequest item);

    /**
     * Removes an item from an open order and restores its inventory.
     * Only allowed while order is PENDING or CONFIRMED.
     *
     * @param orderId     order ID
     * @param orderItemId item to remove
     * @return updated order DTO
     * @throws com.restaurant.rms.exception.InvalidOperationException if order is not in an editable state
     */
    OrderResponse removeItemFromOrder(Long orderId, Long orderItemId);

    /**
     * Returns a single order with all its items.
     *
     * @param id order ID
     * @return full order DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    OrderResponse findById(Long id);

    /**
     * Returns a paginated list of orders with optional filters.
     *
     * @param pageable pagination/sort parameters
     * @param status   optional status filter
     * @param from     optional start date filter (inclusive)
     * @param to       optional end date filter (inclusive)
     * @param tableId  optional table filter
     * @param waiterId optional waiter filter
     * @return paged order list
     */
    PagedResponse<OrderResponse> findAll(Pageable pageable,
                                         OrderStatus status,
                                         LocalDate from,
                                         LocalDate to,
                                         Long tableId,
                                         Long waiterId);

    /**
     * Cancels an order (shortcut for updateOrderStatus → CANCELLED).
     *
     * @param id     order ID
     * @param reason optional cancellation reason
     * @return cancelled order DTO
     */
    OrderResponse cancelOrder(Long id, String reason);

    /**
     * Marks an order as COMPLETED (shortcut for updateOrderStatus → COMPLETED).
     *
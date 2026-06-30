package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.OrderItemStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderItemResponse;
import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.OrderItem;
import com.restaurant.rms.entity.enums.OrderItemStatus;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.OrderItemRepository;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {

    private static final Set<OrderItemStatus> TERMINAL =
            EnumSet.of(OrderItemStatus.SERVED, OrderItemStatus.CANCELLED);

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderItemResponse updateItemStatus(Long orderItemId, OrderItemStatusUpdateRequest request) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));

        OrderItemStatus current = item.getStatus();
        OrderItemStatus next    = request.getStatus();

        if (TERMINAL.contains(current)) {
            throw new InvalidOperationException("updateItemStatus",
                    "item is already in terminal state: " + current);
        }
        // Basic forward-only progression guard
        if (!isValidTransition(current, next)) {
            throw new InvalidOperationException("updateItemStatus",
                    "illegal transition " + current + " → " + next);
        }

        item.setStatus(next);
        OrderItem saved = orderItemRepository.save(item);
        log.debug("Order item id={} status {} → {}", orderItemId, current, next);
        return OrderItemResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemResponse> findByOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderItemRepository.findByOrder(order)
                .stream().map(OrderItemResponse::from).toList();
    }

    private boolean isValidTransition(OrderItemStatus from, OrderItemStatus to) {
        return switch (from) {
            case PENDING   -> to == OrderItemStatus.PREPARING || to == OrderItemStatus.CANCELLED;
            case PREPARING -> to == OrderItemStatus.READY    || to == OrderItemStatus.CANCELLED;
            case READY     -> to == OrderItemStatus.SERVED   || to == OrderItemStat
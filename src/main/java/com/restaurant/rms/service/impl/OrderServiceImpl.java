package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderItemRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.*;
import com.restaurant.rms.entity.enums.*;
import com.restaurant.rms.exception.InsufficientStockException;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.*;
import com.restaurant.rms.service.AuditService;
import com.restaurant.rms.service.OrderService;
import com.restaurant.rms.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    private static final Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.SERVED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryRepository;
    private final MenuItemIngredientRepository ingredientRepository;
    private final AuditService auditService;
    private final WebSocketEventPublisher wsPublisher;

    // ──────────────────────────────────────────────────────────────────
    //  Create Order
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long waiterId) {
        // 1. Validate table
        RestaurantTable table = null;
        if (request.getTableId() != null) {
            table = tableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", request.getTableId()));
            if (table.getStatus() == TableStatus.RESERVED || table.getStatus() == TableStatus.MAINTENANCE) {
                throw new InvalidOperationException("createOrder",
                        "table " + table.getTableNumber() + " is " + table.getStatus());
            }
        }

        // 2. Validate waiter
        User waiter = userRepository.findById(waiterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", waiterId));

        // 3. Build order shell
        Order order = Order.builder()
                .table(table)
                .waiter(waiter)
                .status(OrderStatus.PENDING)
                .orderType(request.getOrderType())
                .notes(request.getNotes())
                .deliveryAddress(request.getDeliveryAddress())
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 4. Validate + create each OrderItem, deduct inventory
        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemReq.getMenuItemId()));
            if (!menuItem.getIsAvailable()) {
                throw new InvalidOperationException("createOrder",
                        "menu item '" + menuItem.getName() + "' is not available");
            }

            // Deduct inventory
            deductInventory(menuItem, itemReq.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .specialNotes(itemReq.getSpecialNotes())
                    .status(OrderItemStatus.PENDING)
                    .build();
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        // 5. Set table to OCCUPIED
        if (table != null) {
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        // 6. Recalculate totals
        recalculateTotal(savedOrder);
        savedOrder = orderRepository.save(savedOrder);

        // 7. WebSocket event
        wsPublisher.publishNewOrder(savedOrder);

        // 8. Audit
        auditService.log("Order", savedOrder.getId(), AuditAction.CREATE,
                null, "status=PENDING table=" + (table != null ? table.getTableNumber() : "N/A"));

        log.info("Created order id={} table={} items={}", savedOrder.getId(),
                table != null ? table.getTableNumber() : "N/A", savedOrder.getItems().size());
        return toResponseWithItems(savedOrder);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Update Order Status
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = getOrderOrThrow(id);
        OrderStatus from = order.getStatus();
        OrderStatus to   = request.getStatus();

        validateTransition(from, to);

        // Restore inventory if cancelling after CONFIRMED
        if (to == OrderStatus.CANCELLED && isAfterConfirmed(from)) {
            restoreInventory(order);
        }

        // Free table if terminal status reached
        if ((to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED)
                && order.getTable() != null) {
            order.getTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(order.getTable());
        }

        order.setStatus(to);
        Order saved = orderRepository.save(order);

        wsPublisher.publishOrderStatusChange(saved);
        auditService.log("Order", id, AuditAction.UPDATE, from.name(), to.name());
        log.info("Order id={} status {} → {}", id, from, to);
        return toResponseWithItems(saved);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Add / Remove Items
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse addItemToOrder(Long orderId, OrderItemRequest itemReq) {
        Order order = getOrderOrThrow(orderId);
        assertEditable(order);

        MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemReq.getMenuItemId()));
        if (!menuItem.getIsAvailable()) {
            throw new InvalidOperationException("addItem", "menu item '" + menuItem.getName() + "' is not available");
        }

        deductInventory(menuItem, itemReq.getQuantity());

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(itemReq.getQuantity())
                .unitPrice(menuItem.getPrice())
                .specialNotes(itemReq.getSpecialNotes())
                .status(OrderItemStatus.PENDING)
                .build();
        orderItemRepository.save(orderItem);
        order.getItems().add(orderItem);

        recalculateTotal(order);
        return toResponseWithItems(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse removeItemFromOrder(Long orderId, Long orderItemId) {
        Order order = getOrderOrThrow(orderId);
        assertEditable(order);

        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", orderItemId));
        if (!item.getOrder().getId().equals(orderId)) {
            throw new InvalidOperationException("removeItem", "item does not belong to order " + orderId);
        }

        // Restore inventory
        restoreInventoryForItem(item.getMenuItem(), item.getQuantity());

        order.getItems().remove(item);
        orderItemRepository.delete(item);

        recalculateTotal(order);
        return toResponseWithItems(orderRepository.save(order));
    }

    // ──────────────────────────────────────────────────────────────────
    //  Queries
    // ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return toResponseWithItems(getOrderOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> findAll(Pageable pageable,
                                                 OrderStatus status,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 Long tableId,
                                                 Long waiterId) {
        List<Order> all = orderRepository.findAll();
        List<Order> filtered = all.stream()
                .filter(o -> status   == null || o.getStatus() == status)
                .filter(o -> tableId  == null || (o.getTable() != null && o.getTable().getId().equals(tableId)))
                .filter(o -> waiterId == null || (o.getWaiter() != null && o.getWaiter().getId().equals(waiterId)))
                .filter(o -> from == null || !o.getCreatedAt().toLocalDate().isBefore(from))
                .filter(o -> to   == null || !o.getCreatedAt().toLocalDate().isAfter(to))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Order> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
        Page<Order> page = new PageImpl<>(pageContent, pageable, filtered.size());
        return PagedResponse.from(page, this::toResponseWithItems);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        OrderStatusUpdateRequest req = new OrderStatusUpdateRequest();
        req.setStatus(OrderStatus.CANCELLED);
        req.setReason(reason);
        return updateOrderStatus(id, req);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(Long id) {
        OrderStatusUpdateRequest req = new OrderStatusUpdateRequest();
        req.setStatus(OrderStatus.COMPLETED);
        return updateOrderStatus(id, req);
    }

    // ──────────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────────

    private void deductInventory(MenuItem menuItem, int quantity) {
        List<MenuItemIngredient> ingredients = ingredientRepository.findByMenuItemId(menuItem.getId());
        for (MenuItemIngredient ing : ingredients) {
            InventoryItem stock = ing.getInventoryItem();
            BigDecimal required = ing.getQuantity().multiply(BigDecimal.valueOf(quantity));
            if (stock.getCurrentStock().compareTo(required) < 0) {
                throw new InsufficientStockException(stock.getName(), required, stock.getCurrentStock());
            }
            stock.setCurrentStock(stock.getCurrentStock().subtract(required));
            inventoryRepository.save(stock);
        }
    }

    private void restoreInventory(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem oi : items) {
            restoreInventoryForItem(oi.getMenuItem(), oi.getQuantity());
        }
    }

    private void restoreInventoryForItem(MenuItem menuItem, int quantity) {
        List<MenuItemIngredient> ingredients = ingredientRepository.findByMenuItemId(menuItem.getId());
        for (MenuItemIngredient ing : ingredients) {
            InventoryItem stock = ing.getInventoryItem();
            BigDecimal toRestore = ing.getQuantity().multiply(BigDecimal.valueOf(quantity));
            stock.setCurrentStock(stock.getCurrentStock().add(toRestore));
            inventoryRepository.save(stock);
        }
    }

    private void recalculateTotal(Order order) {
        List<OrderItem> items = order.getItems().isEmpty()
                ? orderItemRepository.findByOrder(order)
                : order.getItems();
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax   = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        order.setTotalAmount(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING   -> to == OrderStatus.CONFIRMED  || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PREPARING  || to == OrderStatus.CANCELLED;
            case PREPARING -> to == OrderStatus.READY      || to == OrderStatus.CANCELLED;
            case READY     -> to == OrderStatus.SERVED     || to == OrderStatus.CANCELLED;
            case SERVED    -> to == OrderStatus.COMPLETED  || to == OrderStatus.CANCELLED;
            default        -> false;
        };
        if (!valid) {
            throw new InvalidOperationException("updateOrderStatus",
                    "illegal transition " + from + " → " + to);
        }
    }

    private boolean isAfterConfirmed(OrderStatus status) {
        return status == OrderStatus.CONFIRMED || status == OrderStatus.PREPARING
                || status == OrderStatus.READY || status == OrderStatus.SERVED;
    }

    private void assertEditable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("editOrder",
                    "order cannot be edited in status: " + order.getStatus());
        }
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orEls
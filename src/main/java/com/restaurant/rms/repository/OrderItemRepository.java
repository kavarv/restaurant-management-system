package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.OrderItem;
import com.restaurant.rms.entity.enums.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByStatus(OrderItemStatus status);
    List<OrderItem> findByOrderAndStatus(Order order, OrderItemStatus status);
    List<OrderItem> findByOrderIdOrderByMenuItemId(Long orderId);

    /**
     * Most-popular dishes across all time — returns menuItemId + totalQuantity.
     */
    @Query("""
            SELECT oi.menuItem.id, SUM(oi.quantity)
            FROM OrderItem oi
            WHERE oi.status <> com.restaurant.rms.entity.enums.OrderItemStatus.CANCELLED
            GROUP BY oi.menuItem.id
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopMenuItemsByQuantity();

    /**
     * Most-popular dishes within a date range (COMPLETED orders only).
     * Returns Object[]{menuItemId Long, categoryName String,
     *                  totalQuantity Long, totalRevenue BigDecimal}.
     */
    @Query("""
            SELECT oi.menuItem.id,
                   oi.menuItem.category.name,
                   SUM(oi.quantity),
                   SUM(oi.unitPrice * oi.quantity)
            FROM OrderItem oi
            WHERE oi.order.status = com.restaurant.rms.entity.enums.OrderStatus.COMPLETED
              AND oi.order.createdAt BETWEEN :start AND :end
              AND oi.status <> com.restaurant.rms.entity.enums.OrderItemStatus.CANCELLED
            GROUP BY oi.menuItem.id, oi.menuItem.category.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopMenuItemsByQuantityInRange(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end);

    /**
     * Revenue per category for a date range (COMPLETED orders only).
     * Returns Object[]{categoryId Long, categoryName String,
     *                  orderCount Long, totalRevenue BigDecimal}.
     */
    @Query("""
            SELECT oi.menuItem.category.id,
                   oi.menuItem.category.name,
                   COUNT(DISTINCT oi.order.id),
                   SUM(oi.unitPrice * oi.quantity)
            FROM OrderItem oi
            WHERE oi.order.status = com.restaurant.rms.entity.enums.OrderStatus.COMPLETED
              AND oi.order.createdAt BETWEEN :start AND :end
              AND oi.status <> com.restaurant.rms.entity.enums.OrderItemStatus.CANCELLED
            GROUP BY oi.menuItem.category.id, oi.menuItem.category.name
            ORDER BY SUM(oi.unitPrice * oi.quantity) DESC
            """)
    List<Object[]> getRevenueByCategoryInRange(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end);

    @Query("""
            SELECT COUNT(oi) = 0
            FROM OrderItem oi
            WHERE oi.order.id = :orderId
              AND oi.status NOT IN (
                com.restaurant.rms.entity.enums.OrderItemStatus.SERVED,
                com.restaurant.rms.entity.enums.OrderItemStatus.CANCELLED
              )
            """)
    boolean areAllItemsServedOrCancelled(@Param("orderId") Long orderId);

    @Query("""
            SELECT oi FROM OrderItem oi
            WHERE oi.status IN (
                com.restaurant.rms.entity.enums.OrderItemStatus.PENDING,
                com.restaurant.rms.entity.enums.OrderItemStatus.PREPARING
            )
            O
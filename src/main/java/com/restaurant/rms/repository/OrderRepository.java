package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Order;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Kitchen display — all active orders at a given status. */
    List<Order> findByStatus(OrderStatus status);

    /** Paginated status filter for management console. */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Table-level order history — useful when re-opening a tab. */
    List<Order> findByTable(RestaurantTable table);

    List<Order> findByTableId(Long tableId);

    /** Waiter workload view — active orders assigned to a specific waiter. */
    List<Order> findByWaiter(User waiter);

    List<Order> findByWaiterAndStatus(User waiter, OrderStatus status);

    /** Date-range filter for end-of-day / end-of-month reports. */
    Page<Order> findByCreatedAtBetween(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<Order> findByOrderType(OrderType orderType);

    /** Active orders for a table (status not CANCELLED or COMPLETED). */
    List<Order> findByTableIdAndStatusNotIn(Long tableId, List<OrderStatus> excludedStatuses);

    /**
     * Revenue report — total revenue, order count, and average order value
     * for a given time window. Useful for daily/weekly/monthly revenue dashboards.
     *
     * <p>Returns a single Object[] row:
     * {@code [totalRevenue BigDecimal, orderCount Long, avgOrderValue BigDecimal]}</p>
     */
    @Query("""
            SELECT SUM(o.totalAmount), COUNT(o), AVG(o.totalAmount)
            FROM Order o
            WHERE o.status = com.restaurant.rms.entity.enums.OrderStatus.COMPLETED
              AND o.createdAt BETWEEN :start AND :end
            """)
    Object[] getRevenueReport(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Revenue breakdown by order type for the business analytics page.
     * Returns Object[]{orderType VARCHAR, revenue BigDecimal, count LONG}.
     */
    @Query("""
            SELECT o.orderType, SUM(o.totalAmount), COUNT(o)
            FROM Order o
            WHERE o.status = com.restaurant.rms.entity.enums.OrderStatus.COMPLETED
              AND o.createdAt BETWEEN :start AND :end
            GROUP BY o.orderType
            """)
    List<Object[]> getRevenueByOrderType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Daily revenue series — for charting revenue trends.
     * Returns Object[]{date DATE, revenue BigDecimal}.
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS order_date,
                   SUM(o.total_amount)  AS daily_revenue
            FROM orders o
            WHERE o.status = 'COMPLETED'
              AND o.created_at BETWEEN :start AND :end
            GROUP BY DATE(o.created_at)
            ORDER BY order_date
            """, nativeQuery = true)
    List<Object[]> getDailyRevenueSeries(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Count of orders in each status — for the real-time dashboard widget. */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();
}

package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.response.report.*;
import com.restaurant.rms.entity.InventoryItem;
import com.restaurant.rms.entity.MenuItem;
import com.restaurant.rms.repository.InventoryItemRepository;
import com.restaurant.rms.repository.MenuItemRepository;
import com.restaurant.rms.repository.OrderItemRepository;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryItemRepository inventoryRepository;
    private final MenuItemRepository menuItemRepository;

    // ──────────────────────────────────────────────────────────────────────────
    //  Daily report
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DailySalesReport getDailySalesReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay().minusNanos(1);

        Object[] rev = orderRepository.getRevenueReport(start, end);
        // Hibernate 6 wraps multi-column aggregate results in an extra array layer
        Object[] revRow = (rev != null && rev.length > 0 && rev[0] instanceof Object[])
                ? (Object[]) rev[0] : rev;
        BigDecimal totalRevenue  = (revRow != null && revRow[0] != null) ? (BigDecimal) revRow[0] : BigDecimal.ZERO;
        Long       totalOrders   = (revRow != null && revRow[1] != null) ? (Long)       revRow[1] : 0L;
        BigDecimal avgOrderValue = (revRow != null && revRow[2] != null) ? (BigDecimal) revRow[2] : BigDecimal.ZERO;

        // Category breakdown for the same day
        List<Object[]> catRows = orderItemRepository.getRevenueByCategoryInRange(start, end);
        BigDecimal grandTotal  = catRows.stream()
                .map(r -> r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySalesItem> byCategory = catRows.stream()
                .map(r -> CategorySalesItem.builder()
                        .categoryId((Long) r[0])
                        .categoryName((String) r[1])
                        .orderCount((Long) r[2])
                        .revenue(r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO)
                        .build())
                .toList();

        return DailySalesReport.builder()
                .date(date)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(avgOrderValue.setScale(2, RoundingMode.HALF_UP))
                .byCategory(byCategory)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Weekly report
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public WeeklySalesReport getWeeklySalesReport(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        Object[] rev = orderRepository.getRevenueReport(start, end);
        // Hibernate 6 wraps multi-column aggregate results in an extra array layer
        Object[] revRow = (rev != null && rev.length > 0 && rev[0] instanceof Object[])
                ? (Object[]) rev[0] : rev;
        BigDecimal totalRevenue  = (revRow != null && revRow[0] != null) ? (BigDecimal) revRow[0] : BigDecimal.ZERO;
        Long       totalOrders   = (revRow != null && revRow[1] != null) ? (Long)       revRow[1] : 0L;
        BigDecimal avgOrderValue = (revRow != null && revRow[2] != null) ? (BigDecimal) revRow[2] : BigDecimal.ZERO;

        List<Object[]> dailySeries = orderRepository.getDailyRevenueSeries(start, end);
        List<DailySalesItem> dailyBreakdown = dailySeries.stream()
                .map(row -> DailySalesItem.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .revenue((BigDecimal) row[1])
                        .build())
                .toList();

        return WeeklySalesReport.builder()
                .weekStart(startDate)
                .weekEnd(endDate)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(avgOrderValue.setScale(2, RoundingMode.HALF_UP))
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Revenue by category
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryRevenueDTO> getRevenueByCategory(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Object[]> rows = orderItemRepository.getRevenueByCategoryInRange(start, end);

        // Compute grand total for percentage calculation
        BigDecimal grandTotal = rows.stream()
                .map(r -> r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(r -> {
            BigDecimal revenue = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;
            double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : revenue.divide(grandTotal, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                             .doubleValue();
            return CategoryRevenueDTO.builder()
                    .categoryId((Long) r[0])
                    .categoryName((String) r[1])
                    .orderCount((Long) r[2])
                    .totalRevenue(revenue)
                    .percentageOfTotal(Math.round(pct * 100.0) / 100.0)
                    .build();
        }).toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Most-ordered items (with date range)
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TopMenuItemReport> getMostOrderedItems(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end   = (endDate != null)
                ? endDate.plusDays(1).atStartOfDay().minusNanos(1)
                : LocalDateTime.now();

        List<Object[]> rows = orderItemRepository.findTopMenuItemsByQuantityInRange(start, end);

        return rows.stream()
                .limit(limit)
                .map(r -> {
                    Long menuItemId    = (Long) r[0];
                    String catName     = (String) r[1];
                    Long totalQty      = (Long) r[2];
                    BigDecimal revenue = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;

                    MenuItem item = menuItemRepository.findById(menuItemId).orElse(null);
                    return TopMenuItemReport.builder()
                            .menuItemId(menuItemId)
                            .menuItemName(item != null ? item.getName() : "Unknown")
                            .categoryName(catName)
                            .totalQuantity(totalQty)
                            .totalRevenue(revenue)
                            .build();
                })
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Low-stock report
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LowStockReport getLowStockReport() {
        List<InventoryItem> lowItems = inventoryRepository.findAllBelowMinimumStock();
        List<LowStockItem> items = lowItems.stream()
                .map(i -> LowStockItem.builder()
                        .inventoryItemId(i.getId())
                        .name(i.getName())
                        .unit(i.getUnit())
                        .currentStock(i.getCurrentStock())
                        .minimumStock(i.getMinimumStock())
                        .deficit(i.getMinimumStock().subtract(i.getCurrentStock()))
                        .supplierName(i.getSupplierName())
                        .build())
                .toList();
        return LowStockReport.builder()
                .items(items)
                .totalLowStockItems(items.size())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Export-friendly DTO projections
    // ───────────────────────────────────────────────�
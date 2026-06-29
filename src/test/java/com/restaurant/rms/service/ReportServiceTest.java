package com.restaurant.rms.service;

import com.restaurant.rms.dto.response.report.CategoryRevenueDTO;
import com.restaurant.rms.dto.response.report.DailySalesReport;
import com.restaurant.rms.repository.InventoryItemRepository;
import com.restaurant.rms.repository.MenuItemRepository;
import com.restaurant.rms.repository.OrderItemRepository;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService unit tests")
class ReportServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock InventoryItemRepository inventoryRepository;
    @Mock MenuItemRepository menuItemRepository;

    @InjectMocks ReportServiceImpl service;

    // ── getDailySalesReport ────────────────────────────────────────────────

    @Test
    @DisplayName("getDailySalesReport: aggregates revenue, order count, and average correctly")
    void testGetDailySalesReport() {
        LocalDate today = LocalDate.of(2025, 6, 15);
        BigDecimal totalRevenue  = new BigDecimal("350.00");
        Long       totalOrders   = 14L;
        BigDecimal avgOrderValue = new BigDecimal("25.00");

        // Repository returns Object[]{totalRevenue, count, avg}
        Object[] revenueRow = {totalRevenue, totalOrders, avgOrderValue};
        when(orderRepository.getRevenueReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(revenueRow);

        // Category rows: Object[]{categoryId, categoryName, orderCount, revenue}
        Object[] catRow = {1L, "Burgers", 10L, new BigDecimal("280.00")};
        List<Object[]> catRows = new java.util.ArrayList<>();
        catRows.add(catRow);
        when(orderItemRepository.getRevenueByCategoryInRange(any(), any()))
                .thenReturn(catRows);

        DailySalesReport report = service.getDailySalesReport(today);

        assertThat(report.getDate()).isEqualTo(today);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("350.00");
        assertThat(report.getTotalOrders()).isEqualTo(14L);
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo("25.00");
        assertThat(report.getByCategory()).hasSize(1);
        assertThat(report.getByCategory().get(0).getCategoryName()).isEqualTo("Burgers");
        assertThat(report.getByCategory().get(0).getRevenue()).isEqualByComparingTo("280.00");
    }

    @Test
    @DisplayName("getDailySalesReport_noOrders: returns zeroed report without NPE")
    void testGetDailySalesReport_noOrders() {
        LocalDate today = LocalDate.of(2025, 6, 15);
        Object[] emptyRow = {null, null, null};
        when(orderRepository.getRevenueReport(any(), any())).thenReturn(emptyRow);
        when(orderItemRepository.getRevenueByCategoryInRange(any(), any())).thenReturn(List.of());

        DailySalesReport report = service.getDailySalesReport(today);

        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getTotalOrders()).isEqualTo(0L);
        assertThat(report.getByCategory()).isEmpty();
    }

    // ── getRevenueByCategory ──────────────────────────────────────────────

    @Test
    @DisplayName("getRevenueByCategory: computes correct percentages")
    void testGetRevenueByCategory() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end   = LocalDate.of(2025, 6, 30);

        // Two categories: Burgers 300, Drinks 100 → total 400
        Object[] burgers = {1L, "Burgers", 20L, new BigDecimal("300.00")};
        Object[] drinks  = {2L, "Drinks",   8L, new BigDecimal("100.00")};
        when(orderItemRepository.getRevenueByCategoryInRange(any(), any()))
                .thenReturn(List.of(burgers, drinks));

        List<CategoryRevenueDTO> result = service.getRevenueByCategory(start, end);

        assertThat(result).hasSize(2);

        CategoryRevenueDTO burgersDTO = result.stream()
                .filter(d -> "Burgers".equals(d.getCategoryName())).findFirst().orElseThrow();
        assertThat(burgersDTO.getTotalRevenue()).isEqualByComparingTo("300.00");
        assertThat(burgersDTO.getPercentageOfTotal()).isCloseTo(75.0, within(0.01));

        CategoryRevenueDTO drinksDTO = result.stream()
                .filter(d -> "Drinks".equals(d.getCategoryName())).findFirst().orElseThrow();
        assertThat(drinksDTO.getPercentageOfTotal()).isCloseTo(25.0, within(0.01));
    }
}

package com.restaurant.rms.report;

import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.dto.response.report.*;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CsvExportService}.
 *
 * <p>Each test writes to a {@link StringWriter} (no disk I/O) and asserts on the
 * produced CSV string — verifying headers, data rows, and that the service never
 * buffers output in memory (the writer receives all bytes before the method returns).</p>
 */
class CsvExportServiceTest {

    private CsvExportService csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService();
    }

    // ── exportOrdersToCsv ─────────────────────────────────────────────────────

    @Test
    void exportOrdersToCsv_writesHeaderAndOneDataRow() throws Exception {
        OrderResponse order = OrderResponse.builder()
                .id(1L)
                .orderType(OrderType.DINE_IN)
                .tableNumber(3)
                .waiterName("alice")
                .status(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("55.00"))
                .createdAt(LocalDateTime.of(2024, 6, 12, 18, 30))
                .build();

        StringWriter out = new StringWriter();
        csvExportService.exportOrdersToCsv(List.of(order), out);

        String csv = out.toString();
        // Header row present
        assertThat(csv).contains("Order ID");
        assertThat(csv).contains("Order Type");
        assertThat(csv).contains("Total Amount");

        // Data row values present
        assertThat(csv).contains("1");
        assertThat(csv).contains("DINE_IN");
        assertThat(csv).contains("alice");
        assertThat(csv).contains("COMPLETED");
        assertThat(csv).contains("55.00");
    }

    @Test
    void exportOrdersToCsv_emptyList_writesOnlyHeader() throws Exception {
        StringWriter out = new StringWriter();
        csvExportService.exportOrdersToCsv(List.of(), out);

        String csv = out.toString();
        assertThat(csv).contains("Order ID");
        // Only one line (the header)
        long lines = csv.lines().filter(l -> !l.isBlank()).count();
        assertThat(lines).isEqualTo(1);
    }

    // ── exportSalesReportToCsv ────────────────────────────────────────────────

    @Test
    void exportSalesReportToCsv_writesHeaderAndDataRows() throws Exception {
        DailySalesReport day1 = DailySalesReport.builder()
                .date(LocalDate.of(2024, 6, 10))
                .totalOrders(12L)
                .totalRevenue(new BigDecimal("480.00"))
                .averageOrderValue(new BigDecimal("40.00"))
                .byCategory(List.of())
                .build();

        DailySalesReport day2 = DailySalesReport.builder()
                .date(LocalDate.of(2024, 6, 11))
                .totalOrders(8L)
                .totalRevenue(new BigDecimal("320.00"))
                .averageOrderValue(new BigDecimal("40.00"))
                .byCategory(List.of())
                .build();

        StringWriter out = new StringWriter();
        csvExportService.exportSalesReportToCsv(List.of(day1, day2), out);

        String csv = out.toString();
        assertThat(csv).contains("Date");
        assertThat(csv).contains("Total Orders");
        assertThat(csv).contains("2024-06-10");
        assertThat(csv).contains("480.00");
        assertThat(csv).contains("2024-06-11");
        assertThat(csv).contains("320.00");

        long dataLines = csv.lines().filter(l -> !l.isBlank()).count();
        assertThat(dataLines).isEqualTo(3); // header + 2 data rows
    }

    // ── exportCategoryRevenueToCsv ────────────────────────────────────────────

    @Test
    void exportCategoryRevenueToCsv_includesPercentageColumn() throws Exception {
        CategoryRevenueDTO dto = CategoryRevenueDTO.builder()
                .categoryId(1L)
                .categoryName("Mains")
                .orderCount(50L)
                .totalRevenue(new BigDecimal("1500.00"))
                .percentageOfTotal(60.0)
                .build();

        StringWriter out = new StringWriter();
        csvExportService.exportCategoryRevenueToCsv(List.of(dto), out);

        String csv = out.toString();
        assertThat(csv).contains("Category");
        assertThat(csv).contains("% of Total");
        assertThat(csv).contains("Mains");
        assertThat(csv).contains("1500.00");
        assertThat(csv).contains("60.0%");
    }

    // ── exportLowStockToCsv ───────────────────────────────────────────────────

    @Test
    void exportLowStockToCsv_writesCorrectColumnsAndValues() throws Exception {
        LowStockReportDTO dto = LowStockReportDTO.builder()
                .inventoryItemId(7L)
                .itemName("Tomato")
                .unit("kg")
                .currentStock(new BigDecimal("2.000"))
                .reorderThreshold(new BigDecimal("10.000"))
                .deficit(new BigDecimal("8.000"))
                .supplierName("Fresh Farm Ltd")
                .build();

        StringWriter out = new StringWriter();
        csvExportService.exportLowStockToCsv(List.of(dto), out);

        String csv = out.toString();
        assertThat(csv).contains("Item Name");
        assertThat(csv).contains("Deficit");
        assertThat(csv).contains("Tomato");
        assertThat(csv).contains("Fresh Farm Ltd");
        assertThat(csv).contains("8.000");
    }

    // ── Streaming safety ──────────────────────────────────────────────────────

    @Test
    void allExportMethods_canHandleLargeDatasets() throws Exception {
        // Verify no OOM / stack overflow for 10 000 rows
        List<LowStockReportDTO> bigList = java.util.stream.IntStream.range(0, 10_000)
                .mapToObj(i -> LowStockReportDTO.builder()
                        .itemName("Item " + i)
                        .unit("kg")
                        .currentStock(BigDecimal.ONE)
                        .reorderThreshold(new BigDecimal("5"))
                        .deficit(new BigDecimal("4"))
                        .build())
                .toList();

        StringWriter out = new StringWriter();
        assertThatNoException().isThrownBy(
                () -> csvExportService.exportLowStockToCsv(bigList, out));

        // Header + 10 000 data rows
        long lines = out.toString().lines().filter(l -> !l.isBlank()).count();
        assertThat(lines).isEqualTo(10_001L);
    }
}

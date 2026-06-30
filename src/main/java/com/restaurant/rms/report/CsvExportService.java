package com.restaurant.rms.report;

import com.opencsv.CSVWriter;
import com.restaurant.rms.dto.response.OrderResponse;
import com.restaurant.rms.dto.response.report.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Streams CSV data directly to a {@link Writer} (typically backed by
 * {@code HttpServletResponse.getWriter()}) so the JVM never buffers the full
 * export in heap memory — critical for large datasets.
 *
 * <p>All methods write a UTF-8 CSV with a header row, then one data row per
 * record. The caller is responsible for flushing and closing the writer.</p>
 */
@Service
@Slf4j
public class CsvExportService {

    // ── Orders ────────────────────────────────────────────────────────────────

    /**
     * Exports a list of orders as CSV.
     *
     * @param orders flattened order response objects (items not included)
     * @param out    writer backed by the HTTP response stream
     */
    public void exportOrdersToCsv(List<OrderResponse> orders, Writer out) throws IOException {
        try (CSVWriter csv = new CSVWriter(out,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            csv.writeNext(new String[]{
                    "Order ID", "Order Type", "Table Number", "Waiter",
                    "Status", "Total Amount", "Created At"
            });

            for (OrderResponse o : orders) {
                csv.writeNext(new String[]{
                        str(o.getId()),
                        str(o.getOrderType()),
                        str(o.getTableNumber()),
                        str(o.getWaiterName()),
                        str(o.getStatus()),
                        str(o.getTotalAmount()),
                        str(o.getCreatedAt())
                });
            }
        }
        log.debug("CSV export: {} orders written", orders.size());
    }

    // ── Daily sales report ────────────────────────────────────────────────────

    /**
     * Exports one or more daily sales reports as a multi-row CSV.
     * Useful for weekly range exports (one row = one day).
     */
    public void exportSalesReportToCsv(List<DailySalesReport> reports, Writer out) throws IOException {
        try (CSVWriter csv = new CSVWriter(out,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            csv.writeNext(new String[]{
                    "Date", "Total Orders", "Total Revenue", "Average Order Value"
            });

            for (DailySalesReport r : reports) {
                csv.writeNext(new String[]{
                        str(r.getDate()),
                        str(r.getTotalOrders()),
                        str(r.getTotalRevenue()),
                        str(r.getAverageOrderValue())
                });
            }
        }
        log.debug("CSV export: {} daily reports written", reports.size());
    }

    // ── Category revenue ──────────────────────────────────────────────────────

    public void exportCategoryRevenueToCsv(List<CategoryRevenueDTO> rows, Writer out) throws IOException {
        try (CSVWriter csv = new CSVWriter(out,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            csv.writeNext(new String[]{
                    "Category", "Order Count", "Total Revenue", "% of Total"
            });

            for (CategoryRevenueDTO r : rows) {
                csv.writeNext(new String[]{
                        str(r.getCategoryName()),
                        str(r.getOrderCount()),
                        str(r.getTotalRevenue()),
                        r.getPercentageOfTotal() + "%"
                });
            }
        }
        log.debug("CSV export: {} category rows written", rows.size());
    }

    // ── Low-stock report ──────────────────────────────────────────────────────

    /**
     * Exports the low-stock alert list as CSV, suitable for emailing to procurement.
     */
    public void exportLowStockToCsv(List<LowStockReportDTO> items, Writer out) throws IOException {
        try (CSVWriter csv = new CSVWriter(out,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            csv.writeNext(new String[]{
                    "Item Name", "Unit", "Current Stock",
                    "Reorder Threshold", "Deficit", "Supplier"
            });

            for (LowStockReportDTO item : items) {
                csv.writeNext(new String[]{
                        str(item.getItemName()),
                        str(item.getUnit()),
                        str(item.getCurrentStock()),
                        str(item.getReorderThreshold()),
                        str(item.getDeficit()),
                        str(item.getSupplierName())
                });
            }
        }
        log.debug("CSV export: {} low-stock items written", items.size());
    }

    // ── Helper ──────────────────────────────────────────────�
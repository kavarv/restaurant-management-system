package com.restaurant.rms.controller;

import com.restaurant.rms.dto.response.report.*;
import com.restaurant.rms.report.CsvExportService;
import com.restaurant.rms.report.PdfExportService;
import com.restaurant.rms.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * MVC controller for the reporting module.
 *
 * <h3>Streaming design</h3>
 * <p>CSV and PDF export methods write <em>directly</em> to
 * {@code HttpServletResponse.getOutputStream()} / {@code .getWriter()} and
 * never load the entire payload into a {@code byte[]} or {@code String} in memory.
 * This keeps heap usage flat regardless of report size.</p>
 *
 * <h3>Content-Disposition</h3>
 * <p>Both export formats set {@code Content-Disposition: attachment; filename="..."}
 * so the browser immediately prompts a download rather than trying to render inline.</p>
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportController {

    private final ReportService    reportService;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    // ──────────────────────────────────────────────────────────────────────────
    //  HTML views
    // ──────────────────────────────────────────────────────────────────────────

    /** Report landing page — UI is fully JS-driven; no server-side data needed here. */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "reports/index";
    }

    /**
     * Daily report HTML view.
     * Example: {@code GET /reports/daily?date=2024-06-12}
     */
    @GetMapping("/daily")
    public String daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate safeDate = date != null ? date : LocalDate.now();
        DailySalesReport report      = reportService.getDailySalesReport(safeDate);
        List<CategoryRevenueDTO> cats = reportService.getRevenueByCategory(safeDate, safeDate);
        List<TopMenuItemReport> top   = reportService.getMostOrderedItems(10, safeDate, safeDate);

        model.addAttribute("report",      report);
        model.addAttribute("categories",  cats);
        model.addAttribute("topItems",    top);
        return "reports/daily";
    }

    /**
     * Category revenue HTML view.
     * Example: {@code GET /reports/category?from=2024-06-01&to=2024-06-30}
     */
    @GetMapping("/category")
    public String category(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate safeTo   = to   != null ? to   : LocalDate.now();
        model.addAttribute("categories", reportService.getRevenueByCategory(safeFrom, safeTo));
        model.addAttribute("from", safeFrom);
        model.addAttribute("to",   safeTo);
        return "reports/category";
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CSV export  — GET /reports/export?type=csv&report=daily&date=2024-06-12
    //                GET /reports/export?type=csv&report=lowstock
    //                GET /reports/export?type=csv&report=category&from=...&to=...
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Unified export endpoint. Dispatches to CSV or PDF branch based on {@code type}.
     * Streams the response body directly without buffering in memory.
     *
     * <p>Query params:</p>
     * <ul>
     *   <li>{@code type}   — "csv" | "pdf"</li>
     *   <li>{@code report} — "daily" | "category" | "lowstock"</li>
     *   <li>{@code date}   — ISO date for daily report</li>
     *   <li>{@code from}   — ISO date range start (category / orders)</li>
     *   <li>{@code to}     — ISO date range end</li>
     * </ul>
     */
    @GetMapping("/export")
    public void export(
            @RequestParam String type,
            @RequestParam String report,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {

        LocalDate safeDate = date != null ? date : LocalDate.now();
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate safeTo   = to   != null ? to   : LocalDate.now();

        if ("csv".equalsIgnoreCase(type)) {
            exportCsv(report, safeDate, safeFrom, safeTo, response);
        } else if ("pdf".equalsIgnoreCase(type)) {
            exportPdf(report, safeDate, safeFrom, safeTo, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown export type: " + type);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Private — CSV branch
    // ──────────────────────────────────────────────────────────────────────────

    private void exportCsv(String report, LocalDate date, LocalDate from, LocalDate to,
                            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");

        switch (report.toLowerCase()) {
            case "daily" -> {
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"daily-sales-" + date + ".csv\"");
                // Prevent Tomcat from buffering — write directly to response writer
                csvExportService.exportSalesReportToCsv(
                        List.of(reportService.getDailySalesReport(date)),
                        response.getWriter());
            }
            case "category" -> {
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"category-revenue-" + from + "-to-" + to + ".csv\"");
                csvExportService.exportCategoryRevenueToCsv(
                        reportService.getRevenueByCategory(from, to),
                        response.getWriter());
            }
            case "lowstock" -> {
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"low-stock-" + date + ".csv\"");
                csvExportService.exportLowStockToCsv(
                        reportService.getLowStockReportDTOs(),
                        response.getWriter());
            }
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Unknown report type: " + report);
        }
        response.getWriter().flush();
        log.info("CSV export: report={} date={} from={} to={}", report, date, from, to);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Private — PDF branch
    // ──────────────────────────────────────────────────────────────────────────

    private void exportPdf(String report, LocalDate date, LocalDate from, LocalDate to,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");

        switch (report.toLowerCase()) {
            case "daily" -> {
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"daily-sales-" + date + ".pdf\"");
                // Commit header before writing binary body
                pdfExportService.exportSalesReportToPdf(
                        reportService.getDailySalesReport(date),
                        reportService.getRevenueByCategory(date, date),
                        reportService.getMostOrderedItems(10, date, date),
                        response.getOutputStream());
            }
            case "lowstock" -> {
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"low-stock-" + date + ".pdf\"");
                pdfExportService.exportLowStockToPdf(
                        reportService.getLowStockReportDTOs(),
                        response.getOutputStream());
            }
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
               
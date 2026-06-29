package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.response.report.*;
import com.restaurant.rms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportApiController {

    private final ReportService reportService;

    @GetMapping("/daily")
    public ResponseEntity<DailySalesReport> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailySalesReport(date));
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklySalesReport> weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ResponseEntity.ok(reportService.getWeeklySalesReport(startDate));
    }

    @GetMapping("/category-revenue")
    public ResponseEntity<List<CategoryRevenueDTO>> categoryRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getRevenueByCategory(from, to));
    }

    @GetMapping("/top-items")
    public ResponseEntity<List<TopMenuItemReport>> topItems(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getMostOrderedItems(limit, from, to));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<LowStockReport> lowStock() {
        return ResponseEntity.ok(reportService.getLowStockReport());
    }
}

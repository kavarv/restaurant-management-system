package com.restaurant.rms.service;

import com.restaurant.rms.dto.response.report.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    DailySalesReport getDailySalesReport(LocalDate date);

    WeeklySalesReport getWeeklySalesReport(LocalDate startDate);

    /** Revenue by menu category with percentage share. */
    List<CategoryRevenueDTO> getRevenueByCategory(LocalDate startDate, LocalDate endDate);

    List<TopMenuItemReport> getMostOrderedItems(int limit, LocalDate startDate, LocalDate endDate);

    LowStockReport getLowStockReport();

    /** Export-friendly flat DTOs for CSV/PDF low-stock report. */
    List<LowStockReportDTO> getLowStockReportDTOs();
}

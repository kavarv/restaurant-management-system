package com.restaurant.rms.controller;

import com.restaurant.rms.service.InventoryService;
import com.restaurant.rms.service.OrderService;
import com.restaurant.rms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * Handles pages under /manager/**.
 *
 * MANAGERs can also reach /inventory and /reports (covered by SecurityConfig and
 * their own controllers), but this controller owns the manager-specific landing page
 * that {@link CustomAuthenticationSuccessHandler} redirects to after login.
 */
@Controller
@RequestMapping("/manager")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class ManagerController {

    private final OrderService     orderService;
    private final ReportService    reportService;
    private final InventoryService inventoryService;

    /**
     * Landing page after MANAGER login.
     * Shows today's revenue, active orders, and low-stock alerts.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        model.addAttribute("dailyReport",   reportService.getDailySalesReport(today));
        model.addAttribute("lowStockItems", inventoryService.findLowStockItems());
        model.addAttribute("activeOrders",  orderService.findAll(
                PageRequest.of(0, 10), null, today, today, null, null));

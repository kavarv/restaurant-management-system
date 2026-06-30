package com.restaurant.rms.controller;

import com.restaurant.rms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the billing / checkout UI page.
 * Actual payment processing is delegated to {@link com.restaurant.rms.controller.api.PaymentApiController}
 * via the client-side JavaScript in billing/checkout.html.
 */
@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
public class BillingController {

    private final OrderService orderService;

    @GetMapping("/checkout/{orderId}")
    public String checkout(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.findById(orderId));
        return
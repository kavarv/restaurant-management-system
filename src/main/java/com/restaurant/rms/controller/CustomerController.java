package com.restaurant.rms.controller;

import com.restaurant.rms.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the customer-facing panel pages.
 * All data is fetched client-side via the REST API — these methods only
 * return the template name and inject the current username for the greeting.
 */
@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getUsername() : "Guest");
        return "customer/dashboard";
    }

    @GetMapping("/menu")
    public String menu() {
        return "customer/menu";
    }

    @GetMapping("/reserve")
    public String reserve() {
        return "customer/reserve";
    }

    @GetMapping("/reservations")
    public String reservations() {
        return "customer/reservations";
    }
}

package com.restaurant.rms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the public-facing landing page at /.
 * No authentication required — accessible to all visitors.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String landingPage() {
        return "in
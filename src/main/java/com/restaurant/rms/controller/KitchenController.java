package com.restaurant.rms.controller;

import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/kitchen")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','CHEF')")
public class KitchenController {

    private final OrderService orderService;

    @GetMapping
    public String kitchenDisplay(Model model) {
        model.addAttribute("pendingOrders", orderService.findAll(
                PageRequest.of(0, 50), OrderStatus.PENDING, null, null, null, null).getContent());
        model.addAttribute("confirmedOrders", orderService.findAll(
                PageRequest.of(0, 50), OrderStatus.CONFIRMED, null, null, null, null).getContent());
        model.addAttribute("preparingOrders", orderService.findAll(
                PageRequest.of(0, 50), OrderStatus.PREPARING, null, null, null, null).getContent());
        model.addAttribute("readyOrders", orderService.findAll(
                PageRequest.of(0, 50), OrderStatus.READY, null, null, null, null).getContent());
        return "dashboard/chef";
    }
}

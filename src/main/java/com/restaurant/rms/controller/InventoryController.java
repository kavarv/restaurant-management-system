package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.AdjustStockRequest;
import com.restaurant.rms.dto.request.InventoryItemRequest;
import com.restaurant.rms.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", inventoryService.findAll());
        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String lowStock(Model model) {
        model.addAttribute("items", inventoryService.findLowStockItems());
        return "inventory/low-stock";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("inventoryItemRequest", new InventoryItemRequest());
        return "inventory/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute InventoryItemRequest inventoryItemRequest,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "inventory/form";
        try {
            inventoryService.create(inventoryItemRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Inventory item created.");
        } catch (Exception ex) {
            log.warn("Inventory create failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", inventoryService.findById(id));
        model.addAttribute("inventoryItemRequest", new InventoryItemRequest());
        return "inventory/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute InventoryItemRequest inventoryItemRequest,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "inventory/form";
        try {
            inventoryService.update(id, inventoryItemRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Inventory item updated.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/{id}/adjust")
    public String adjustForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", inventoryService.findById(id));
        model.addAttribute("adjustStockRequest", new AdjustStockRequest());
        return "inventory/adjust";
    }

    @PostMapping("/{id}/adjust")
    public String adjust(@PathVariable Long id,
                         @Valid @ModelAttribute AdjustStockRequest adjustStockRequest,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "inventory/adjust";
        try {
            inventoryService.adjustStock(id, adjustStockRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Stock adjusted.");
        } catch (Exception ex) {
            log.warn("Stock adjust failed: {}", ex.getMessage());
            redirectAttrs.addFlas
package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.TableRequest;
import com.restaurant.rms.service.TableService;
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
@RequestMapping("/tables")
@RequiredArgsConstructor
@Slf4j
public class TableController {

    private final TableService tableService;

    @GetMapping
    public String floorView(Model model) {
        model.addAttribute("tables", tableService.findAll());
        return "tables/floor";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String newForm(Model model) {
        model.addAttribute("tableRequest", new TableRequest());
        return "tables/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String create(@Valid @ModelAttribute TableRequest tableRequest,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "tables/form";
        try {
            tableService.create(tableRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Table created.");
        } catch (Exception ex) {
            log.warn("Table create failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/tables";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("table", tableService.findById(id));
        model.addAttribute("tableRequest", new TableRequest());
        return "tables/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute TableRequest tableRequest,
                         BindingResult result,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) return "tables/form";
        try {
            tableService.update(id, tableRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Table updated.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("errorMsg", e
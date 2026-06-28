package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.service.CategoryService;
import com.restaurant.rms.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuItemService menuItemService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String search,
                       Model model) {
        model.addAttribute("menuItems", menuItemService.findAll(
                PageRequest.of(page, size), categoryId, null, search));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("search", search);
        return "menu/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String newForm(Model model) {
        model.addAttribute("menuItemRequest", new MenuItemRequest());
        model.addAttribute("categories", categoryService.findAll());
        return "menu/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String create(@Valid @ModelAttribute MenuItemRequest menuItemRequest,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "menu/form";
        }
        try {
            menuItemService.create(menuItemRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Menu item created.");
        } catch (Exception ex) {
            log.warn("Menu item creation failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/menu";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuItemService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        return "menu/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute MenuItemRequest menuItemRequest,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "menu/form";
        }
        try {
            menuItemService.update(id, menuItemRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Menu item updated.");
        } catch (Exception ex) {
            log.warn("Menu item update failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/menu";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            menuItemService.delete(id);
            redirectAttrs.addFlashAttribute("successMsg", "Menu item removed.");
        } catch (Exception ex) {
            log.warn("Menu item delete failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/menu";
    }
}

package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.CreateOrderRequest;
import com.restaurant.rms.dto.request.OrderStatusUpdateRequest;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.security.UserPrincipal;
import com.restaurant.rms.service.MenuItemService;
import com.restaurant.rms.service.OrderService;
import com.restaurant.rms.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final TableService tableService;
    private final MenuItemService menuItemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("orders", orderService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                null, null, null, null, null));
        return "orders/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    public String newForm(Model model) {
        model.addAttribute("createOrderRequest", new CreateOrderRequest());
        model.addAttribute("tables", tableService.findAvailable());
        model.addAttribute("menuItems", menuItemService.findAll(PageRequest.of(0, 200), null, true, null));
        return "orders/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','WAITER')")
    public String create(@Valid @ModelAttribute CreateOrderRequest createOrderRequest,
                         BindingResult result,
                         // @AuthenticationPrincipal injects the UserPrincipal set by
                         // UserDetailsServiceImpl — gives us the DB id without a second query.
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("tables", tableService.findAvailable());
            return "orders/form";
        }
        try {
            // Resolve the waiter: explicit override in the form wins (manager assigning
            // an order to another waiter), otherwise use the logged-in user's own id.
            Long waiterId = createOrderRequest.getWaiterId() != null
                    ? createOrderRequest.getWaiterId()
                    : principal.getId();   // no hardcoded fallback — auth is required by @PreAuthorize

            var created = orderService.createOrder(createOrderRequest, waiterId);
            redirectAttrs.addFlashAttribute("successMsg", "Order #" + created.getId() + " created.");
            return "redirect:/orders/" + created.getId();
        } catch (Exception ex) {
            log.warn("Order creation failed: {}", ex.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
            return "redirect:/orders/new";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMI
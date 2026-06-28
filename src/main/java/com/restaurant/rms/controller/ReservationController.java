package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.ReservationRequest;
import com.restaurant.rms.service.ReservationService;
import com.restaurant.rms.service.TableService;
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
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;
    private final TableService tableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("reservations",
                reservationService.findAll(PageRequest.of(page, size), null, null));
        return "reservations/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("reservationRequest", new ReservationRequest());
        model.addAttribute("tables", tableService.findAll());
        return "reservations/booking";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ReservationRequest reservationRequest,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("tables", tableService.findAll());
            return "reservations/booking";
        }
        try {
            var created = reservationService.create(reservationRequest,
                    reservationRequest.getCustomerId());
            redirectAttrs.addFlashAttribute("successMsg",
                    "Reservation created. Code: " + created.getConfirmationCode());
            return "redirect:/reservations";
        } catch (Exception ex) {
            log.warn("Reservation creation failed: {}", ex.getMessage());
            result.reject("error", ex.getMessage());
            model.addAttribute("tables", tableService.findAll());
            return "reservations/booking";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationService.findById(id));
        return "reservations/detail";
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            reservationService.confirm(id);
            redirectAttrs.addFlashAttribute("successMsg", "Reservation confirmed.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/reservations/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            reservationService.cancel(id);
            redirectAttrs.addFlashAttribute("successMsg", "Reservation cancelled.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/reservations";
    }
}

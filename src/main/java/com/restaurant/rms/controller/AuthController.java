package com.restaurant.rms.controller;

import com.restaurant.rms.dto.request.RegisterRequest;
import com.restaurant.rms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("errorMsg", "Invalid username or password.");
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult result,
                           RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(registerRequest);
            redirectAttrs.addFlashAttribute("successMsg", "Account created — please log in.");
            return "redirect:/login";
        } catch (Exception ex) {
            log.warn("Registration failed: {}", ex.getMessage());
            result.reject("registration.failed", ex.getMessage());
     
package com.restaurant.rms.controller;

import com.restaurant.rms.dto.response.AuditLogResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.enums.Role;
import com.restaurant.rms.repository.AuditLogRepository;
import com.restaurant.rms.repository.UserRepository;
import com.restaurant.rms.service.*;
import com.restaurant.rms.service.impl.MenuItemServiceImpl;
import com.restaurant.rms.util.AuditDiffUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Admin-only MVC controller.
 *
 * <p>Class-level {@code @PreAuthorize("hasRole('ADMIN')")} locks every method.
 * This mirrors the SecurityConfig URL rule and adds a second enforcement layer
 * at the method level so no individual handler can accidentally open a page.</p>
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService         userService;
    private final UserRepository      userRepository;
    private final OrderService        orderService;
    private final ReportService       reportService;
    private final InventoryService    inventoryService;
    private final AuditService        auditService;
    private final AuditLogRepository  auditLogRepository;
    private final MenuItemServiceImpl menuItemService;
    private final AuditDiffUtil       auditDiffUtil;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers",    userRepository.count());
        model.addAttribute("adminCount",    userRepository.findByRole(Role.ADMIN).size());
        model.addAttribute("managerCount",  userRepository.findByRole(Role.MANAGER).size());
        model.addAttribute("waiterCount",   userRepository.findByRole(Role.WAITER).size());
        model.addAttribute("chefCount",     userRepository.findByRole(Role.CHEF).size());
        model.addAttribute("customerCount", userRepository.findByRole(Role.CUSTOMER).size());

        LocalDate today = LocalDate.now();
        model.addAttribute("dailyReport",   reportService.getDailySalesReport(today));
        model.addAttribute("lowStockItems", inventoryService.findLowStockItems());
        model.addAttribute("activeOrders",  orderService.findAll(
                PageRequest.of(0, 5), null, today, today, null, null));
        model.addAttribute("recentAudit",   auditService.findRecent(10));
        return "admin/dashboard";
    }

    // ── User management ───────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {
        model.addAttribute("users", userService.findAll(PageRequest.of(page, size)));
        return "admin/users";
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return "redirect:/admin/users";
    }

    // ── Soft-deleted menu items ───────────────────────────────────────────────

    /**
     * Lists all soft-deleted menu items so admins can restore them.
     * Route: {@code GET /admin/menu/deleted}
     */
    @GetMapping("/menu/deleted")
    public String deletedMenuItems(Model model) {
        model.addAttribute("deletedItems", menuItemService.findAllDeleted());
        return "admin/menu-deleted";
    }

    /**
     * Restores a soft-deleted menu item.
     * Route: {@code POST /admin/menu/{id}/restore}
     */
    @PostMapping("/menu/{id}/restore")
    public String restoreMenuItem(@PathVariable Long id) {
        menuItemService.restore(id);
        return "redirect:/admin/menu/deleted";
    }

    // ── Audit log — list ──────────────────────────────────────────────────────

    /**
     * Paginated, filterable audit log view.
     * Route: {@code GET /admin/audit?entityType=MenuItem&from=2024-06-01&to=2024-06-30&userId=5&page=0}
     */
    @GetMapping("/audit")
    public String auditIndex(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end   = to   != null ? to.plusDays(1).atStartOfDay().minusNanos(1) : null;

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"));
        var logs = auditLogRepository.findFiltered(entityType, userId, start, end, pageable);

        model.addAttribute("logs",        logs.map(AuditLogResponse::from));
        model.addAttribute("entityType",  entityType);
        model.addAttribute("from",        from);
        model.addAttribute("to",          to);
        model.addAttribute("userId",      userId);
        model.addAttribute("currentPage", page);
        return "admin/audit/index";
    }

    // ── Audit log — entity history ────────────────────────────────────────────

    /**
     * Full change history for a specific entity with JSON diff.
     * Route: {@code GET /admin/audit/entity/MenuItem/42}
     */
    @GetMapping("/audit/entity/{type}/{id}")
    public String entityHistory(
            @PathVariable String type,
            @PathVariable Long   id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        PagedResponse<AuditLogResponse> history =
                auditService.findByEntity(type, id,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt")));

        // Build diff for each audit entry
        var diffs = history.getContent().stream()
                .map(entry -> auditDiffUtil.compareJsonValues(entry.getOldValues(), entry.getNewValues()))
                .toList();

        model.addAttribute("history",    history);
        model.addAttribute("diffs",      diffs);
        model.addAttribute("entityType", type);
        model.addAttribute("entityId",   id);
        return "admin/audit/entity-history";
    }

    // ── Audit log — user activity ─────────────────────────────────────────────

    /**
     * All audit entries created by a specific user.
     * Route: {@code GET /admin/audit/user/5}
     */
    @GetMapping("/audit/user/{userId}")
    public String userActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        PagedResponse<AuditLogResponse> activity =
                auditService.findByUser(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Dire
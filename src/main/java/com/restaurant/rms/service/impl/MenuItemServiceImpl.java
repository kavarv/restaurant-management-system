package com.restaurant.rms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.Category;
import com.restaurant.rms.entity.MenuItem;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.CategoryRepository;
import com.restaurant.rms.repository.MenuItemRepository;
import com.restaurant.rms.service.AuditService;
import com.restaurant.rms.service.MenuItemService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        Category category = getCategoryOrThrow(request.getCategoryId());
        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .isVegetarian(request.getIsVegetarian() != null ? request.getIsVegetarian() : false)
                .isVegan(request.getIsVegan() != null ? request.getIsVegan() : false)
                .isGlutenFree(request.getIsGlutenFree() != null ? request.getIsGlutenFree() : false)
                .imageUrl(request.getImageUrl())
                .preparationTimeMinutes(request.getPreparationTimeMinutes())
                .calories(request.getCalories())
                .build();
        MenuItem saved = menuItemRepository.save(item);

        auditService.log("MenuItem", saved.getId(), AuditAction.CREATE,
                null, toSnapshot(saved), currentRequest());
        log.info("Created menu item id={} name={}", saved.getId(), saved.getName());
        return MenuItemResponse.from(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = getItemOrThrow(id);

        // Capture full JSON snapshot BEFORE mutation
        Object oldSnapshot = toSnapshot(item);

        Category category = getCategoryOrThrow(request.getCategoryId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        if (request.getIsAvailable()           != null) item.setIsAvailable(request.getIsAvailable());
        if (request.getIsVegetarian()          != null) item.setIsVegetarian(request.getIsVegetarian());
        if (request.getIsVegan()               != null) item.setIsVegan(request.getIsVegan());
        if (request.getIsGlutenFree()          != null) item.setIsGlutenFree(request.getIsGlutenFree());
        if (request.getImageUrl()              != null) item.setImageUrl(request.getImageUrl());
        if (request.getPreparationTimeMinutes()!= null) item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        if (request.getCalories()              != null) item.setCalories(request.getCalories());

        MenuItem saved = menuItemRepository.save(item);

        auditService.log("MenuItem", saved.getId(), AuditAction.UPDATE,
                oldSnapshot, toSnapshot(saved), currentRequest());
        return MenuItemResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse findById(Long id) {
        return MenuItemResponse.from(getItemOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MenuItemResponse> findAll(Pageable pageable,
                                                    Long categoryId,
                                                    Boolean isAvailable,
                                                    String searchTerm) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM MenuItem m WHERE m.deletedAt IS NULL");
        if (categoryId  != null) jpql.append(" AND m.category.id = :categoryId");
        if (isAvailable != null) jpql.append(" AND m.isAvailable = :isAvailable");
        if (searchTerm  != null && !searchTerm.isBlank())
            jpql.append(" AND (LOWER(m.name) LIKE LOWER(CONCAT('%',:term,'%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%',:term,'%')))");

        var query      = entityManager.createQuery(jpql.toString(), MenuItem.class);
        var countQuery = entityManager.createQuery(
                jpql.toString().replace("SELECT m", "SELECT COUNT(m)"), Long.class);

        if (categoryId  != null) { query.setParameter("categoryId", categoryId);  countQuery.setParameter("categoryId", categoryId); }
        if (isAvailable != null) { query.setParameter("isAvailable", isAvailable); countQuery.setParameter("isAvailable", isAvailable); }
        if (searchTerm  != null && !searchTerm.isBlank()) {
            query.setParameter("term", searchTerm);
            countQuery.setParameter("term", searchTerm);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<MenuItem> results = query.getResultList();
        long total = countQuery.getSingleResult();
        Page<MenuItem> page = new PageImpl<>(results, pageable, total);
        return PagedResponse.from(page, MenuItemResponse::from);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MenuItem item = getItemOrThrow(id);
        if (item.isDeleted()) {
            throw new InvalidOperationException("deleteMenuItem", "item is already soft-deleted");
        }

        // Full snapshot before marking deleted
        Object oldSnapshot = toSnapshot(item);

        item.setDeletedAt(LocalDateTime.now());
        menuItemRepository.save(item);

        auditService.log("MenuItem", id, AuditAction.DELETE,
                oldSnapshot, null, currentRequest());
        log.info("Soft-deleted menu item id={}", id);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public MenuItemResponse restore(Long id) {
        // @SQLRestriction filters soft-deleted rows — use native query to bypass it
        var results = entityManager.createNativeQuery(
                "SELECT * FROM menu_items WHERE id = :id AND deleted_at IS NOT NULL",
                MenuItem.class)
                .setParameter("id", id)
                .getResultList();

        if (results.isEmpty()) {
            menuItemRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
            throw new InvalidOperationException("restoreMenuItem", "item is not currently deleted");
        }
        MenuItem item = (MenuItem) results.get(0);
        if (!item.isDeleted()) {
            throw new InvalidOperationException("restoreMenuItem", "item is not currently deleted");
        }

        Object oldSnapshot = toSnapshot(item); // state while deleted
        item.setDeletedAt(null);
        MenuItem saved = menuItemRepository.save(item);

        auditService.log("MenuItem", id, AuditAction.RESTORE,
                oldSnapshot, toSnapshot(saved), currentRequest());
        log.info("Restored menu item id={}", id);
        return MenuItemResponse.from(saved);
    }

    // ── Admin: list soft-deleted items ────────────────────────────────────────

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<MenuItemResponse> findAllDeleted() {
        return entityManager
                .createNativeQuery("SELECT * FROM menu_items WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC",
                        MenuItem.class)
                .getResultList()
                .stream()
                .map(obj -> MenuItemResponse.from((MenuItem) obj))
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MenuItem getItemOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    /**
     * Produces a serialization-safe snapshot map of a MenuItem.
     * We build a plain Map rather than serializing the entity directly to avoid
     * lazy-load proxies being serialized by Jackson.
     */
    private Map<String, Object> toSnapshot(MenuItem item) {
        if (item == null) return null;
        return Map.of(
                "id",          item.getId() != null ? item.getId() : 0L,
                "name",        item.getName() != null ? item.getName() : "",
                "description", item.getDescription() != null ? item.getDescription() : "",
                "price",       item.getPrice() != null ? item.getPrice().toPlainString() : "0",
                "categoryId",  item.getCategory() != null ? item.getCategory().getId() : 0L,
                "isAvailable", Boolean.TRUE.equals(item.getIsAvailable()),
                "isVegetarian",Boolean.TRUE.equals(item.getIsVegetarian()),
                "deletedAt",   item.getDeletedAt() != null ? item.getDeletedAt().toString() : ""
        );
    }

    /** Retrieves the current HTTP request from the Spring request context (null-safe). */
    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
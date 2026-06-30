package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Manages the restaurant menu (create, update, soft-delete, restore, search).
 */
public interface MenuItemService {

    /**
     * Creates a new menu item and logs a CREATE audit entry.
     *
     * @param request menu item details
     * @return created menu item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if the referenced category does not exist
     */
    MenuItemResponse create(MenuItemRequest request);

    /**
     * Updates an existing menu item and logs an UPDATE audit entry.
     *
     * @param id      menu item ID
     * @param request new values
     * @return updated menu item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found or soft-deleted
     */
    MenuItemResponse update(Long id, MenuItemRequest request);

    /**
     * Returns a single menu item by ID.
     *
     * @param id menu item ID
     * @return menu item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found or soft-deleted
     */
    MenuItemResponse findById(Long id);

    /**
     * Returns a filtered, paginated list of menu items.
     *
     * @param pageable   pagination and sort parameters
     * @param categoryId optional category filter (null = all categories)
     * @param isAvailable optional availability filter (null = all)
     * @param searchTerm  optional name/description search (null = no filter)
     * @return paged menu item list
     */
    PagedResponse<MenuItemResponse> findAll(Pageable pageable,
                                            Long categoryId,
                                            Boolean isAvailable,
                                            String searchTerm);

    /**
     * Soft-deletes a menu item (sets deletedAt = now) and logs a DELETE audit entry.
     * The item will no longer appear in any standard query.
     *
     * @param id menu item ID
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found or already deleted
     */
    void delete(Long id);

    /**
     * Restores a previously soft-deleted menu item (sets deletedAt = null)
     * and logs a RESTORE audit entry.
     *
     * @param id menu item ID (must reference a soft-deleted row)
     * @return restored menu item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if the item is n
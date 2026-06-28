package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.AdjustStockRequest;
import com.restaurant.rms.dto.request.InventoryItemRequest;
import com.restaurant.rms.dto.response.InventoryItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Manages raw ingredient and supply inventory.
 */
public interface InventoryService {

    /**
     * Creates a new inventory item.
     *
     * @param request item details
     * @return created item DTO
     * @throws com.restaurant.rms.exception.DuplicateResourceException if name already exists
     */
    InventoryItemResponse create(InventoryItemRequest request);

    /**
     * Updates an existing inventory item's metadata (name, unit, thresholds, cost, supplier).
     *
     * @param id      inventory item ID
     * @param request new values
     * @return updated item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    InventoryItemResponse update(Long id, InventoryItemRequest request);

    /**
     * Adjusts the current stock level of an item.
     * Positive quantity = credit (received stock); negative = debit (consumption/waste).
     * Logs old and new stock values via {@link AuditService}.
     *
     * @param id      inventory item ID
     * @param request adjustment quantity and reason
     * @return updated item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if resulting stock would drop below zero
     */
    InventoryItemResponse adjustStock(Long id, AdjustStockRequest request);

    /**
     * Returns all inventory items (non-paginated, for internal use / low-volume contexts).
     *
     * @return list of all items
     */
    List<InventoryItemResponse> findAll();

    /**
     * Returns a paginated slice of inventory items.
     *
     * <p>Use this overload from API controllers to avoid loading the entire table
     * into memory.  Typical caller: {@code GET /api/v1/inventory?page=0&size=20&sort=name,asc}</p>
     *
     * @param pageable page number, size, and sort — resolved from request params by Spring MVC
     * @return paged response containing the DTO slice and pagination metadata
     */
    PagedResponse<InventoryItemResponse> findAll(Pageable pageable);

    /**
     * Returns a single inventory item by ID.
     *
     * @param id inventory item ID
     * @return item DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    InventoryItemResponse findById(Long id);

    /**
     * Returns all items whose current stock is below their minimum threshold.
     *
     * @return list of low-stock items
     */
    List<InventoryItemResponse> findLowStockItems();
}

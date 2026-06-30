package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.CategoryRequest;
import com.restaurant.rms.dto.response.CategoryResponse;

import java.util.List;

/**
 * Manages menu categories (Starters, Mains, Desserts, etc.).
 */
public interface CategoryService {

    /**
     * Creates a new menu category.
     *
     * @param request category details
     * @return created category DTO
     * @throws com.restaurant.rms.exception.DuplicateResourceException if name already exists
     */
    CategoryResponse create(CategoryRequest request);

    /**
     * Updates an existing category.
     *
     * @param id      category ID
     * @param request new values
     * @return updated category DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    CategoryResponse update(Long id, CategoryRequest request);

    /**
     * Returns all categories ordered by displayOrder.
     *
     * @return list of all categories (active and inactive)
     */
    List<CategoryResponse> findAll();

    /**
     * Returns a single category by ID.
     *
     * @param id category ID
     * @return category DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    CategoryResponse findById(Long id);

    /**
     * Permanently deletes a category.
     *
     * @param id category ID
     * @throws com.restaurant.rms.exception.ResourceNotFoundException  if not found
     * @throws com.restaurant.rms.exception.InvalidOperationException  if menu items still referenc
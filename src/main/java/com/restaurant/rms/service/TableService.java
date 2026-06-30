package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.TableRequest;
import com.restaurant.rms.dto.response.TableResponse;
import com.restaurant.rms.entity.enums.TableStatus;

import java.util.List;

/**
 * Manages physical restaurant tables (CRUD + availability queries).
 */
public interface TableService {

    /**
     * Creates a new table.
     *
     * @param request table details
     * @return created table DTO
     * @throws com.restaurant.rms.exception.DuplicateResourceException if table number already exists
     */
    TableResponse create(TableRequest request);

    /**
     * Updates an existing table.
     *
     * @param id      table ID
     * @param request new values
     * @return updated table DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    TableResponse update(Long id, TableRequest request);

    /**
     * Returns all tables.
     *
     * @return list of all tables
     */
    List<TableResponse> findAll();

    /**
     * Returns a single table by ID.
     *
     * @param id table ID
     * @return table DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    TableResponse findById(Long id);

    /**
     * Returns all tables currently in AVAILABLE status.
     *
     * @return list of available tables
     */
    List<TableResponse> findAvailable();

    /**
     * Updates the status of a table (e.g. AVAILABLE → OCCUPIED).
     *
     * @param id     table ID
     * @param status new status
     * @return updated table DTO
     */
    TableResponse updateStatus(Long id, TableStatus status);

    /**
     * Deletes a table (only if it has no active orders).
     *
     * @param id table ID
     * @throws com.restaurant.rms.exception.InvalidOperationExcep
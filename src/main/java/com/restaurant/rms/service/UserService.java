package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.RegisterRequest;
import com.restaurant.rms.dto.request.UpdateUserRequest;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

/**
 * Manages user accounts (registration, lookup, updates, deactivation).
 * Authentication itself is delegated to Spring Security.
 */
public interface UserService {

    /**
     * Registers a new user account, hashing the password before persisting.
     *
     * @param request registration payload
     * @return the created user's public profile
     * @throws com.restaurant.rms.exception.DuplicateResourceException if username or email already exists
     */
    UserResponse register(RegisterRequest request);

    /**
     * Looks up a user by primary key.
     *
     * @param id user ID
     * @return user profile DTO
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    UserResponse findById(Long id);

    /**
     * Returns a paginated list of all users.
     *
     * @param pageable pagination/sort parameters
     * @return paged user list
     */
    PagedResponse<UserResponse> findAll(Pageable pageable);

    /**
     * Updates mutable fields (email, phone, password) of an existing user.
     *
     * @param id      user ID
     * @param request fields to update (nulls are ignored)
     * @return updated user profile
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     */
    UserResponse updateUser(Long id, UpdateUserRequest request);

    /**
     * Soft-deactivates a user account (sets isActive = false).
     *
     * @param id user ID
     * @throws com.restaurant.rms.exception.ResourceNotFoundException if not found
     * @throws com.restaurant.rms.exception.InvalidOperationException if already inactive
     */
    void deactivateUser(Long id);
}

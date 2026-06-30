package com.restaurant.rms.repository;

import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>Method-name derivation rules used here:
 * <ul>
 *   <li>{@code findBy<Field>} → SELECT … WHERE field = ?</li>
 *   <li>{@code existsBy<Field>} → SELECT COUNT(*) > 0 WHERE field = ?</li>
 *   <li>{@code findBy<Field>AndIsActiveTrue} → adds an AND is_active = 1 predicate</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used by Spring Security's UserDetailsService to load a user for authentication.
     */
    Optional<User> findByUsername(String username);

    /**
     * Used by the registration flow to look up a user by email
     * (alternative login identifier).
     */
    Optional<User> findByEmail(String email);

    /**
     * Pre-registration duplicate check.
     */
    boolean existsByUsername(String username);

    /**
     * Pre-registration duplicate check.
     */
    boolean existsByEmail(String email);

    /**
     * Loads all active staff members for a given role (e.g. to assign a waiter
     * to a new order, or to list all managers for a report).
     */
    List<User> findByRoleAndIsActiveTrue(Role role);

    /**
     * Admin user-management screen — list all users with a given role.
     */
    List<User> findByRole(Role role);

    /**
     * Soft-deactivation check — confirms the account is not already inactive
     * before triggering a deactivation flow.
     
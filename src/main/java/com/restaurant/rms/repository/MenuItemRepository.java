package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Category;
import com.restaurant.rms.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for {@link MenuItem}.
 *
 * <p>Because MenuItem extends SoftDeletableEntity, Hibernate automatically appends
 * {@code AND deleted_at IS NULL} to every query defined here — soft-deleted items
 * are completely transparent to callers.  The {@code findByDeletedAtIsNull} method
 * is therefore redundant at the Hibernate level but is kept as explicit documentation
 * of the intent and for use by a raw JPA query that bypasses the restriction.</p>
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /** Menu browsing by category. */
    List<MenuItem> findByCategory(Category category);

    List<MenuItem> findByCategoryId(Long categoryId);

    /** Front-of-house menu display — only currently available items. */
    List<MenuItem> findByIsAvailableTrue();

    /** Paginated menu management screen (admin). */
    Page<MenuItem> findByIsAvailableTrue(Pageable pageable);

    /**
     * Paginated listing of all non-deleted items regardless of availability
     * (admin view). The @SQLRestriction already filters soft-deleted rows;
     * this method name documents the explicit intent.
     */
    @Query("SELECT m FROM MenuItem m WHERE m.deletedAt IS NULL")
    Page<MenuItem> findByDeletedAtIsNull(Pageable pageable);

    /** Search bar on the menu screen — case-insensitive partial name match. */
    Page<MenuItem> findByNameContainingIgnoreCaseAndIsAvailableTrue(
            String name, Pageable pageable);

    /** Full admin search including unavailable items. */
    Page<MenuItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** Dietary filter — customers filtering for vegetarian options. */
    List<MenuItem> findByIsVegetarianTrueAndIsAvailableTrue();

    List<MenuItem> findByIsVeganTrueAndIsAvailableTrue();

    List<MenuItem> findByIsGlutenFreeTrueAndIsAvailableTrue();

    /** Price range filter. */
    List<MenuItem> findByPriceBetweenAndIsAvailableTrue(
            BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Most-ordered items — used for "Popular dishes" section.
     * Joins to order_items, groups by menu item, orders by total quantity sold.
     */
    @Query("""
            SELECT m FROM MenuItem m
            JOIN m.orderItems oi
            WHERE m.isAvailable = true
            GROUP BY m.id
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<MenuItem> findTopSellingItems(Pageable pageable);

    /**
     * Full-text search across name and description (LIKE-based; replace with
     * FULLTEXT index + MATCH AGAINST for production scale).
     */
    @Query("""
            SELECT m FROM MenuItem m
            WHERE (LOWER(m.name) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :term, '%')))
              AND m.isAvailable = t
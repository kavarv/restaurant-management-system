package com.restaurant.rms.repository;

import com.restaurant.rms.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByName(String name);

    boolean existsByName(String name);

    /**
     * Low-stock alert — returns all items whose current stock is below the
     * given threshold.  Called by a scheduled job with {@code threshold = item.minimumStock}.
     */
    List<InventoryItem> findByCurrentStockLessThan(BigDecimal threshold);

    /**
     * Low-stock alert using each item's own minimum threshold —
     * more useful than a fixed threshold because each item has a different safety level.
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.currentStock < i.minimumStock")
    List<InventoryItem> findAllBelowMinimumStock();

    /**
     * Search bar in inventory management screen.
     */
    List<InventoryItem> findByNameContainingIgnoreCase(String name);

    /**
     * Supplier-based filter for procurement reports.
     */
    List<InventoryItem> findBySupplierName(String supplierName);

    /**
     * Stock valuation report — total cost of on-hand inventory.
     * Returns a single Double (currentStock × costPerUnit summed across all rows).
     */
    @Query("SELECT SUM(i.currentStock * i.costPerUnit) FROM InventoryItem i")
    BigDecimal calculateTotalInventoryValue();

    /**
     * Ingredient lookup used by the service layer when processing an order —
     * checks whether enough stock exists before decrementing.
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.id IN :ids AND i.currentStock < i.minimumStock")
    List<InventoryIte
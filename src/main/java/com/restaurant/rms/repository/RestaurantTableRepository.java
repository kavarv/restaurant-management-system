package com.restaurant.rms.repository;

import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findByTableNumber(Integer tableNumber);

    boolean existsByTableNumber(Integer tableNumber);

    List<RestaurantTable> findByStatus(TableStatus status);

    /** Quick host-stand view — all tables that can seat a given party right now. */
    List<RestaurantTable> findByStatusAndCapacityGreaterThanEqual(
            TableStatus status, Integer minCapacity);

    /**
     * Dashboard count — how many tables are in each status bucket.
     * Returns Object[]{status VARCHAR, count BIGINT}.
     */
    @Query("SELECT t.status, COUNT(t) FROM RestaurantTable t GROUP BY t.status")
    List<Object[
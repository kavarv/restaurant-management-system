package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    /** Menu display — active categories ordered by their display priority. */
    List<Category> findByIsActiveTrueOrderByDisp
package com.restaurant.rms.repository;

import com.restaurant.rms.entity.MenuItemIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {

    List<MenuItemIngredient> findByMenuItemId(Long menuItemId);

    List<MenuItemIngredient> findByInventoryItemId(Long inventoryItemId);

    void deleteByMenuItemId(Long menuItemId);
}

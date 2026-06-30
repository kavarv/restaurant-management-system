package com.restaurant.rms.util.mapper;

import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.entity.Category;
import com.restaurant.rms.entity.MenuItem;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for {@link MenuItem} ↔ DTO conversions.
 */
@Component
public class MenuItemMapper {

    public MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.from(item);
    }

    public MenuItem toEntity(MenuItemRequest request, Category category) {
        return MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .isVegetarian(request.getIsVegetarian() != null ? request.getIsVegetarian() : false)
                .isVegan(request.getIsVegan() != null ? request.getIsVegan() : false)
                .isGlutenFree(request.getIsGlutenFree() != null ? request.getIsGlutenFree() : false)
                .imageUrl(request.getImageUrl())
                .preparationTimeMinutes(request.getPreparationTimeMinutes())
                .calories(request.getCalories())
                .build();
    }

    public void updateEntity(MenuItem item, MenuItemRequest request, Category category) {
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        if (request.getIsAvailable()  != null) item.setIsAvailable(request.getIsAvailable());
        if (request.getIsVegetarian() != null) item.setIsVegetarian(request.getIsVegetarian());
        if (request.getIsVegan()      != null) item.setIsVegan(request.getIsVegan());
        if (request.getIsGlutenFree() != null) item.setIsGlutenFree(request.getIsGlutenFree());
        if (request.getImageUrl()     != null) item.setImageUrl(request.getImageUrl());
        if (request.getPreparationTimeMinutes() != null) item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        if (request.getCalories()     != null) item
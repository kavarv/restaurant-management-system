package com.restaurant.rms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Create / update payload for a MenuItem.
 */
@Data
@Schema(description = "Request body for creating or updating a menu item")
public class MenuItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    @Schema(description = "Display name of the menu item", example = "Classic Beef Burger", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Detailed description shown to customers", example = "Juicy beef patty with lettuce, tomato, and our house sauce on a brioche bun")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
    @Schema(description = "Selling price in the restaurant's currency", example = "12.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be a positive number")
    @Schema(description = "ID of the category this item belongs to", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "Whether the item is currently available to order", example = "true", defaultValue = "true")
    private Boolean isAvailable = true;

    @Schema(description = "True if the item contains no meat", example = "false", defaultValue = "false")
    private Boolean isVegetarian = false;

    @Schema(description = "True if the item contains no animal products", example = "false", defaultValue = "false")
    private Boolean isVegan = false;

    @Schema(description = "True if the item is gluten-free", example = "false", defaultValue = "false")
    private Boolean isGlutenFree = false;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "URL to the item's photograph", example = "https://cdn.example.com/images/burger.jpg")
    private String imageUrl;

    @PositiveOrZero(message = "Preparation time cannot be negative")
    @Max(value = 300, message = "Preparation time cannot exceed 300 minutes")
    @Schema(description = "Estimated preparation time in minutes", example = "15")
    private Integer preparationTimeMinutes;

    @PositiveOrZero(message = "Calories cannot be negative")
    @Max(value = 5000, message = "Calorie count seems unrealistically high")
    @Schema(description = "Approximate calorie count per s
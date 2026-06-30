package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "menu", description = "Menu item management — create, read, update, soft-delete, restore")
public class MenuItemApiController {

    private final MenuItemService menuItemService;

    @GetMapping
    @Operation(
        summary     = "List all menu items",
        description = "Returns a paginated list of active menu items. Supports filtering by category, " +
                      "availability, and full-text search across name and description."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of menu items returned successfully",
                     content = @Content(schema = @Schema(implementation = PagedResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter", content = @Content)
    })
    public ResponseEntity<PagedResponse<MenuItemResponse>> list(
            @Parameter(description = "Zero-based page index",  example = "0")
            @RequestParam(defaultValue = "0")  int page,

            @Parameter(description = "Page size (max items per page)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(defaultValue = "name") String sort,

            @Parameter(description = "Filter by category ID", example = "3")
            @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Full-text search across name and description", example = "burger")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by availability (true = available)", example = "true")
            @RequestParam(required = false) Boolean available) {

        PagedResponse<MenuItemResponse> result = menuItemService.findAll(
                PageRequest.of(page, size, Sort.by(sort)),
                categoryId, available, search);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get menu item by ID", description = "Returns a single active menu item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu item found",
                     content = @Content(schema = @Schema(implementation = MenuItemResponse.class))),
        @ApiResponse(responseCode = "404", description = "Menu item not found",    content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",      content = @Content)
    })
    public ResponseEntity<MenuItemResponse> getById(
            @Parameter(description = "Menu item ID", example = "42") @PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
        summary     = "Create a menu item",
        description = "Creates a new menu item. Requires ADMIN or MANAGER role.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Menu item details",
            required    = true,
            content     = @Content(schema = @Schema(implementation = MenuItemRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Menu item created; Location header set",
                     content = @Content(schema = @Schema(implementation = MenuItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error — check fieldErrors",  content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",                     content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role (WAITER, CHEF)",      content = @Content)
    })
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = menuItemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/menu/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
        summary     = "Update a menu item",
        description = "Replaces all fields of an existing menu item. Requires ADMIN or MANAGER role.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated menu item payload", required = true,
            content     = @Content(schema = @Schema(implementation = MenuItemRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu item updated",
                     content = @Content(schema = @Schema(implementation = MenuItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",  content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found",         content = @Content)
    })
    public ResponseEntity<MenuItemResponse> update(
            @Parameter(description = "Menu item ID", example = "42") @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
        summary     = "Soft-delete a menu item",
        description = "Marks the item as deleted (sets deletedAt). The item is hidden from the public " +
                      "menu but can be restored. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Soft-deleted successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Item is already deleted",   content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",         content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role",         content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found",                 content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Menu item ID", example = "42") @PathVariable Long id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(
        summary     = "Restore a soft-deleted menu item",
        description = "Clears the deletedAt timestamp, making the item visible again. " +
                      "Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu item restored",
                     content = @Content(schema = @Schema(implementation = MenuItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Item is not currently deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated",             content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role",             content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found",                     content = @Content)
    })
    public ResponseEntity<MenuItemResponse> restore(
 
package com.restaurant.rms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paged response wrapper for any paginated list endpoint.
 *
 * <p>Usage in a service or controller (before MapStruct is added):</p>
 * <pre>{@code
 *   Page<MenuItem> page = menuItemRepository.findByIsAvailableTrue(pageable);
 *   return PagedResponse.from(page, MenuItemResponse::from);
 * }</pre>
 *
 * @param <T> the DTO type contained in this page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;

    /**
     * Factory method that converts a Spring Data {@link Page} of entities into a
     * {@code PagedResponse} of DTOs using the supplied mapping function.
     *
     * @param page    the Spring Data page result
     * @param mapper  a function that converts each entity to its DTO
     * @param <E>     entity type
     * @param <D>     DTO type
     */
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return PagedResponse.<D>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .l
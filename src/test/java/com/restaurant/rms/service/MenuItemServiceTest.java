package com.restaurant.rms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.rms.dto.request.MenuItemRequest;
import com.restaurant.rms.dto.response.MenuItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.Category;
import com.restaurant.rms.entity.MenuItem;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.CategoryRepository;
import com.restaurant.rms.repository.MenuItemRepository;
import com.restaurant.rms.service.impl.MenuItemServiceImpl;
import com.restaurant.rms.util.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuItemService unit tests")
class MenuItemServiceTest {

    @Mock MenuItemRepository menuItemRepo;
    @Mock CategoryRepository categoryRepo;
    @Mock AuditService auditService;
    @Mock EntityManager entityManager;

    // ObjectMapper is a concrete class — use @Mock so Mockito stubs it
    @Mock ObjectMapper objectMapper;

    @InjectMocks
    MenuItemServiceImpl service;

    private Category category;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        category = TestDataFactory.category(1L, "Burgers");
        item     = TestDataFactory.menuItem(10L, "Classic Burger", new BigDecimal("12.99"), category);
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createMenuItem_success: saves item and returns response with correct fields")
    void testCreateMenuItem_success() {
        MenuItemRequest request = TestDataFactory.menuItemRequest("Classic Burger", new BigDecimal("12.99"), 1L);

        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(menuItemRepo.save(any(MenuItem.class))).thenReturn(item);

        MenuItemResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Classic Burger");
        assertThat(response.getPrice()).isEqualByComparingTo("12.99");
        verify(menuItemRepo).save(any(MenuItem.class));
        verify(auditService).log(eq("MenuItem"), eq(10L), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createMenuItem_categoryNotFound: throws ResourceNotFoundException")
    void testCreateMenuItem_categoryNotFound() {
        MenuItemRequest request = TestDataFactory.menuItemRequest();
        when(categoryRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");

        verify(menuItemRepo, never()).save(any());
    }

    // ── delete (soft-delete) ─────────────────────────────────────────────

    @Test
    @DisplayName("softDelete_success: sets deletedAt and logs audit event")
    void testSoftDelete_success() {
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(item));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(10L);

        assertThat(item.getDeletedAt()).isNotNull();
        verify(auditService).log(eq("MenuItem"), eq(10L), any(), any(), isNull(), any());
    }

    @Test
    @DisplayName("softDelete_alreadyDeleted: throws InvalidOperationException")
    void testSoftDelete_alreadyDeleted() {
        item.setDeletedAt(LocalDateTime.now().minusDays(1));
        when(menuItemRepo.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(com.restaurant.rms.exception.InvalidOperationException.class)
                .hasMessageContaining("already soft-deleted");
    }

    // ── findAll with filters ─────────────────────────────────────────────

    @Test
    @DisplayName("findAll_withFilters: returns correct paged response")
    void testFindAll_withFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        // EntityManager query chain — use lenient stubbing for the complex chain
        jakarta.persistence.TypedQuery<MenuItem> typedQuery =
                mock(jakarta.persistence.TypedQuery.class);
        jakarta.persistence.TypedQuery<Long> countQuery =
                mock(jakarta.persistence.TypedQuery.class);

        when(entityManager.createQuery(contains("SELECT m"), eq(MenuItem.class))).thenReturn(typedQuery);
        when(entityManager.createQuery(contains("SELECT COUNT"), eq(Long.class))).thenReturn(countQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(item));
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);

        PagedResponse<MenuItemResponse> page = service.findAll(pageable, 1L, true, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Classic Burger");
    }

    // ── restore ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("restore_onDeletedItem: clears deletedAt and logs restore")
    void testRestore_onDeletedItem() {
        item.setDeletedAt(LocalDateTime.now().minusDays(2));

        jakarta.persistence.Query nativeQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(MenuItem.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(item));
        when(menuItemRepo.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse response = service.restore(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(item.getDeletedAt()).isNull();
        verify(auditService).log(eq("MenuItem"), eq(10L), any(), any(), any(), any());
    }
}

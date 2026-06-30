package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.AdjustStockRequest;
import com.restaurant.rms.dto.response.InventoryItemResponse;
import com.restaurant.rms.entity.InventoryItem;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.InventoryItemRepository;
import com.restaurant.rms.service.impl.InventoryServiceImpl;
import com.restaurant.rms.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService unit tests")
class InventoryServiceTest {

    @Mock InventoryItemRepository inventoryRepository;
    @Mock AuditService auditService;

    @InjectMocks InventoryServiceImpl service;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = TestDataFactory.inventoryItem(7L, "Tomato", new BigDecimal("50.000"), new BigDecimal("5.000"));
    }

    // ── adjustStock ───────────────────────────────────────────────────────

    @Test
    @DisplayName("adjustStock_debit: stock reduced by the requested amount")
    void testAdjustStock_debit() {
        AdjustStockRequest req = new AdjustStockRequest();
        req.setQuantity(new BigDecimal("-10.000"));
        req.setReason("Spoilage");

        when(inventoryRepository.findById(7L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItemResponse response = service.adjustStock(7L, req);

        assertThat(response.getCurrentStock()).isEqualByComparingTo("40.000");
        verify(inventoryRepository).save(item);
    }

    @Test
    @DisplayName("adjustStock_belowZero: throws InvalidOperationException")
    void testAdjustStock_belowZero() {
        AdjustStockRequest req = new AdjustStockRequest();
        req.setQuantity(new BigDecimal("-999.000"));
        req.setReason("Bad adjustment");

        when(inventoryRepository.findById(7L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.adjustStock(7L, req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot go below zero");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock_itemNotFound: throws ResourceNotFoundException")
    void testAdjustStock_itemNotFound() {
        AdjustStockRequest req = new AdjustStockRequest();
        req.setQuantity(new BigDecimal("5.000"));
        req.setReason("Restock");

        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjustStock(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("InventoryItem");
    }

    // ── findLowStockItems ─────────────────────────────────────────────────

    @Test
    @DisplayName("findLowStockItems: returns only items at or below minimum threshold")
    void testFindLowStockItems() {
        InventoryItem normal = TestDataFactory.inventoryItem(8L, "Onion",
                new BigDecimal("20.000"), new BigDecimal("5.000")); // 20 > 5, not low

        InventoryItem low = TestDataFactory.inventoryItem(9L, "Olive Oil",
                new BigDecimal("2.000"), new BigDecimal("5.000")); // 2 < 5, low

        when(inventoryRepository.findAllBelowMinimumStock()).thenReturn(List.of(low));

        List<InventoryItemResponse> result = service.findLowStockItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Olive Oil");
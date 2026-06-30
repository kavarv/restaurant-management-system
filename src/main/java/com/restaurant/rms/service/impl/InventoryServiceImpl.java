package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.AdjustStockRequest;
import com.restaurant.rms.dto.request.InventoryItemRequest;
import com.restaurant.rms.dto.response.InventoryItemResponse;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.entity.InventoryItem;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.exception.DuplicateResourceException;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.InventoryItemRepository;
import com.restaurant.rms.service.AuditService;
import com.restaurant.rms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        if (inventoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("InventoryItem", "name", request.getName());
        }
        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .unit(request.getUnit())
                .currentStock(request.getCurrentStock())
                .minimumStock(request.getMinimumStock())
                .costPerUnit(request.getCostPerUnit())
                .supplierName(request.getSupplierName())
                .build();
        InventoryItem saved = inventoryRepository.save(item);
        auditService.log("InventoryItem", saved.getId(), AuditAction.CREATE, null, saved.getName());
        return InventoryItemResponse.from(saved);
    }

    @Override
    @Transactional
    public InventoryItemResponse update(Long id, InventoryItemRequest request) {
        InventoryItem item = getItemOrThrow(id);
        if (!item.getName().equals(request.getName()) && inventoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("InventoryItem", "name", request.getName());
        }
        item.setName(request.getName());
        item.setUnit(request.getUnit());
        item.setMinimumStock(request.getMinimumStock());
        item.setCostPerUnit(request.getCostPerUnit());
        item.setSupplierName(request.getSupplierName());
        return InventoryItemResponse.from(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryItemResponse adjustStock(Long id, AdjustStockRequest request) {
        InventoryItem item = getItemOrThrow(id);
        BigDecimal oldStock = item.getCurrentStock();
        BigDecimal newStock = oldStock.add(request.getQuantity());
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("adjustStock",
                    "resulting stock " + newStock + " cannot go below zero");
        }
        item.setCurrentStock(newStock);
        InventoryItem saved = inventoryRepository.save(item);
        auditService.log("InventoryItem", id, AuditAction.UPDATE,
                "stock=" + oldStock, "stock=" + newStock + " reason=" + request.getReason());
        log.info("Adjusted stock for item id={}: {} → {} ({})",
                id, oldStock, newStock, request.getReason());
        return InventoryItemResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findAll() {
        return inventoryRepository.findAll().stream().map(InventoryItemResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryItemResponse> findAll(Pageable pageable) {
        return PagedResponse.from(
                inventoryRepository.findAll(pageable),
                InventoryItemResponse::from
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse findById(Long id) {
        return InventoryItemResponse.from(getItemOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findLowStockItems() {
        return inventoryRepository.findAllBelowMinimumStock()
                .stream().map(InventoryItemResponse::from).toList();
    }

    private InventoryItem getItemOrThrow(Long id) {
        return inventoryRepository.fi
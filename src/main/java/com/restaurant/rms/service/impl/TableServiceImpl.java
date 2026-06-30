package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.TableRequest;
import com.restaurant.rms.dto.response.TableResponse;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.OrderStatus;
import com.restaurant.rms.entity.enums.TableStatus;
import com.restaurant.rms.exception.DuplicateResourceException;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.OrderRepository;
import com.restaurant.rms.repository.RestaurantTableRepository;
import com.restaurant.rms.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableServiceImpl implements TableService {

    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public TableResponse create(TableRequest request) {
        if (tableRepository.existsByTableNumber(request.getTableNumber())) {
            throw new DuplicateResourceException("RestaurantTable", "tableNumber", request.getTableNumber());
        }
        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : TableStatus.AVAILABLE)
                .locationDescription(request.getLocationDescription())
                .build();
        return TableResponse.from(tableRepository.save(table));
    }

    @Override
    @Transactional
    public TableResponse update(Long id, TableRequest request) {
        RestaurantTable table = getTableOrThrow(id);
        if (!table.getTableNumber().equals(request.getTableNumber()) &&
                tableRepository.existsByTableNumber(request.getTableNumber())) {
            throw new DuplicateResourceException("RestaurantTable", "tableNumber", request.getTableNumber());
        }
        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        if (request.getStatus() != null) table.setStatus(request.getStatus());
        table.setLocationDescription(request.getLocationDescription());
        return TableResponse.from(tableRepository.save(table));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> findAll() {
        return tableRepository.findAll().stream().map(TableResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TableResponse findById(Long id) {
        return TableResponse.from(getTableOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableResponse> findAvailable() {
        return tableRepository.findByStatus(TableStatus.AVAILABLE)
                .stream().map(TableResponse::from).toList();
    }

    @Override
    @Transactional
    public TableResponse updateStatus(Long id, TableStatus status) {
        RestaurantTable table = getTableOrThrow(id);
        table.setStatus(status);
        return TableResponse.from(tableRepository.save(table));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RestaurantTable table = getTableOrThrow(id);
        List<OrderStatus> active = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED);
        boolean hasActiveOrders = orderRepository.findByTableId(id).stream()
                .anyMatch(o -> active.contains(o.getStatus()));
        if (hasActiveOrders) {
            throw new InvalidOperationException("deleteTable", "table has active orders");
        }
        tableRepository.delete(table);
    }

    private RestaurantTable getTableOrThrow(Long id) {
        return tableRepository.findById(id)
   
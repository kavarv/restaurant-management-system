package com.restaurant.rms.controller.api;

import com.restaurant.rms.dto.request.TableRequest;
import com.restaurant.rms.dto.response.TableResponse;
import com.restaurant.rms.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class TableApiController {

    private final TableService tableService;

    @GetMapping
    public ResponseEntity<List<TableResponse>> list() {
        return ResponseEntity.ok(tableService.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TableResponse>> available() {
        return ResponseEntity.ok(tableService.findAvailable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TableResponse> create(@Valid @RequestBody TableRequest request) {
        TableResponse created = tableService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tables/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TableResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody TableRequest request) {
        return ResponseEntity.ok(tableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

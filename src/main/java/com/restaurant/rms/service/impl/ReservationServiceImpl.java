package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.ReservationRequest;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.dto.response.ReservationResponse;
import com.restaurant.rms.entity.Reservation;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.ReservationStatus;
import com.restaurant.rms.entity.enums.TableStatus;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.ReservationRepository;
import com.restaurant.rms.repository.RestaurantTableRepository;
import com.restaurant.rms.repository.UserRepository;
import com.restaurant.rms.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository     reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository            userRepository;

    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request, Long callerId) {

        // ── Resolve table ────────────────────────────────────────────────────
        RestaurantTable table;
        if (request.getTableId() != null) {
            table = tableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "RestaurantTable", "id", request.getTableId()));
        } else {
            // Auto-select: first AVAILABLE table that fits the party, no conflicts
            table = findAvailableTable(request.getPartySize(), request.getReservedDate());
        }

        // ── Conflict check ───────────────────────────────────────────────────
        LocalDateTime windowStart = request.getReservedDate().minusHours(2);
        LocalDateTime windowEnd   = request.getReservedDate().plusHours(2);
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                table.getId(), windowStart, windowEnd);
        if (!conflicts.isEmpty()) {
            throw new InvalidOperationException("createReservation",
                    "Table " + table.getTableNumber() + " already has a booking in this time window");
        }

        // ── Resolve customer (optional) ──────────────────────────────────────
        Long resolvedCustomerId = request.getCustomerId() != null
                ? request.getCustomerId() : callerId;
        User customer = null;
        if (resolvedCustomerId != null) {
            customer = userRepository.findById(resolvedCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", resolvedCustomerId));
        } else if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new InvalidOperationException("createReservation",
                    "Either a logged-in account or a customer name is required");
        }

        // ── Build and save ───────────────────────────────────────────────────
        Reservation reservation = Reservation.builder()
                .customer(customer)
                .customerName(customer == null ? request.getCustomerName() : null)
                .customerEmail(customer == null ? request.getCustomerEmail() : null)
                .customerPhone(customer == null ? request.getCustomerPhone() : null)
                .table(table)
                .reservedDate(request.getReservedDate())
                .partySize(request.getPartySize())
                .status(ReservationStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created id={} table={} date={} customer={}",
                saved.getId(), table.getTableNumber(),
                request.getReservedDate(),
                customer != null ? customer.getUsername() : request.getCustomerName());
        return ReservationResponse.from(saved);
    }

    /**
     * Finds the first AVAILABLE table that seats at least {@code partySize} guests
     * and has no conflicting reservation in the ±2-hour window around {@code when}.
     */
    private RestaurantTable findAvailableTable(int partySize, LocalDateTime when) {
        LocalDateTime windowStart = when.minusHours(2);
        LocalDateTime windowEnd   = when.plusHours(2);

        return tableRepository
                .findByStatusAndCapacityGreaterThanEqual(TableStatus.AVAILABLE, partySize)
                .stream()
                .filter(t -> reservationRepository
                        .findConflictingReservations(t.getId(), windowStart, windowEnd)
                        .isEmpty())
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("createReservation",
                        "No available tables for " + partySize + " guests at that time. " +
                        "Please choose a different date or time."));
    }

    @Override
    @Transactional
    public ReservationResponse confirm(Long id) {
        Reservation reservation = getOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException("confirmReservation",
                    "Can only confirm PENDING reservations, current: " + reservation.getStatus());
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = getOrThrow(id);
        if (reservation.getStatus() == ReservationStatus.COMPLETED
                || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidOperationException("cancelReservation",
                    "Reservation is already " + reservation.getStatus());
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> findAll(Pageable pageable,
                                                       LocalDate date,
                                                       ReservationStatus status) {
        List<Reservation> all = reservationRepository.findAll();
        List<Reservation> filtered = all.stream()
                .filter(r -> date   == null || r.getReservedDate().toLocalDate().equals(date))
                .filter(r -> status == null || r.getStatus() == status)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Reservation> pageContent = start < filtered.size()
                ? filtered.subList(start, end) : List.of();

        Page<Reservation> page = new PageImpl<>(pageContent, pageable, filtered.size());
        return PagedResponse.from(page, ReservationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        return ReservationResponse.from(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> findByCustomer(Long customerId, Pageable pageable) {
        List<Reservation> all =
                reservationRepository.findByCustomerIdOrderByReservedDateDesc(customerId);
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), all.size());
        List<Reservation> pageContent = start < all.size() ? all.subList(start, end) : List.of();
        Page<Reservation> page = new PageImpl<>(pageContent, pageable, all.size());
        return PagedResponse.from(page, ReservationResponse::from);
    }

    private Reservation getOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }
}

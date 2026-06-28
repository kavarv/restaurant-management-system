package com.restaurant.rms.service;

import com.restaurant.rms.dto.request.ReservationRequest;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.dto.response.ReservationResponse;
import com.restaurant.rms.entity.enums.ReservationStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Manages table reservations (create, confirm, cancel, query).
 */
public interface ReservationService {

    ReservationResponse create(ReservationRequest request, Long customerId);

    ReservationResponse confirm(Long id);

    ReservationResponse cancel(Long id);

    PagedResponse<ReservationResponse> findAll(Pageable pageable,
                                               LocalDate date,
                                               ReservationStatus status);

    ReservationResponse findById(Long id);

    /**
     * Returns all reservations belonging to a specific customer, newest first.
     *
     * @param customerId ID of the authenticated customer
     * @param pageable   pagination parameters
     * @return paged list of the customer's reservations
     */
    PagedResponse<ReservationResponse> findByCustomer(Long customerId, Pageable pageable);
}

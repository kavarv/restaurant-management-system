package com.restaurant.rms.repository;

import com.restaurant.rms.entity.Reservation;
import com.restaurant.rms.entity.RestaurantTable;
import com.restaurant.rms.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Daily booking sheet — all reservations for a specific date window. */
    List<Reservation> findByReservedDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Availability check — is this table already booked at the requested time?
     * The service passes the desired slot ± 2 hours as the window.
     */
    List<Reservation> findByTableAndReservedDateAndStatus(
            RestaurantTable table,
            LocalDateTime reservedDate,
            ReservationStatus status);

    /**
     * Overlap detection — finds any CONFIRMED or PENDING reservation for a table
     * whose time window overlaps the requested window.  Used before confirming
     * a new reservation to prevent double-booking.
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.table.id = :tableId
              AND r.status IN (
                  com.restaurant.rms.entity.enums.ReservationStatus.PENDING,
                  com.restaurant.rms.entity.enums.ReservationStatus.CONFIRMED
              )
              AND r.reservedDate BETWEEN :windowStart AND :windowEnd
            """)
    List<Reservation> findConflictingReservations(
            @Param("tableId") Long tableId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

    /** Customer reservation history (guest-facing "My Bookings" screen). */
    List<Reservation> findByCustomerIdOrderByReservedDateDesc(Long customerId);

    List<Reservation> findByCustomerIdAndStatus(Long customerId, ReservationStatus status);

    /** Upcoming reservations for today's service — host stand view. */
    List<Reservation> findByReservedDateBetweenAndStatusOrderByReservedDateAsc(
            LocalDateTime start, LocalDateTime end, ReservationStatus status);

    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    /** No-show sweep — CONFIRMED reservations where the slot has already passed. */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.status = com.restaurant.rms.entity.enums.ReservationStatus.CONFIRMED
              AND r.reservedDate < :now
            """)
    List<Reservation> findOverdueConfirmedReservations(@Param("now") LocalDateTime now);
}

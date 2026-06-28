package com.restaurant.rms.dto.response;

import com.restaurant.rms.entity.Reservation;
import com.restaurant.rms.entity.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long tableId;
    private Integer tableNumber;
    private Integer tableCapacity;
    private LocalDateTime reservedDate;
    private Integer partySize;
    private ReservationStatus status;
    private String notes;
    private String confirmationCode;
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation res) {
        // Prefer linked User account details; fall back to plain string fields
        // for anonymous (public) reservations.
        String name  = res.getCustomer() != null
                ? res.getCustomer().getUsername() : res.getCustomerName();
        String email = res.getCustomer() != null
                ? res.getCustomer().getEmail()    : res.getCustomerEmail();
        String phone = res.getCustomer() != null
                ? res.getCustomer().getPhone()    : res.getCustomerPhone();

        return ReservationResponse.builder()
                .id(res.getId())
                .customerId(res.getCustomer() != null ? res.getCustomer().getId() : null)
                .customerName(name)
                .customerEmail(email)
                .customerPhone(phone)
                .tableId(res.getTable() != null ? res.getTable().getId() : null)
                .tableNumber(res.getTable() != null ? res.getTable().getTableNumber() : null)
                .tableCapacity(res.getTable() != null ? res.getTable().getCapacity() : null)
                .reservedDate(res.getReservedDate())
                .partySize(res.getPartySize())
                .status(res.getStatus())
                .notes(res.getNotes())
                .confirmationCode(res.getConfirmationCode())
                .createdAt(res.getCreatedAt())
                .build();
    }
}

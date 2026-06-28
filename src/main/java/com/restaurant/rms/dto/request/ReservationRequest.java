package com.restaurant.rms.dto.request;

import com.restaurant.rms.dto.validation.ValidTableCapacity;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Payload for POST /api/v1/reservations.
 *
 * Supports two booking modes:
 *  - Staff / logged-in:  provide customerId (links to a User account)
 *  - Public / anonymous: provide customerName + customerPhone (no account needed)
 *
 * tableId is optional — when absent the service auto-selects the first
 * available table that fits the party.
 */
@Data
public class ReservationRequest {

    /** Optional — when absent the service picks the best available table. */
    @Positive(message = "Table ID must be a positive number")
    private Long tableId;

    /** Optional — for staff creating a reservation on behalf of an account holder. */
    private Long customerId;

    /** Required for public (anonymous) bookings when customerId is absent. */
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String customerName;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Email(message = "Must be a valid email address")
    private String customerEmail;

    @Pattern(
        regexp = "^(\\+\\d{1,3}[- ]?)?\\d{7,15}$",
        message = "Phone number is not valid"
    )
    private String customerPhone;

    @NotNull(message = "Reservation date/time is required")
    @Future(message = "Reservation must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime reservedDate;

    @NotNull(message = "Party size is required")
    @ValidTableCapacity(min = 1, max = 20,
                        message = "Party size must be between 1 and 20")
    private Integer partySize;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}

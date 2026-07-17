package be.condorcet.easycarrent.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request payload used to create or update a {@code Rental}.
 *
 * <p>The client supplies only the booking period and the referenced vehicle and
 * customer. The identifier, status, total price and creation timestamp are all
 * controlled by the backend and are intentionally not part of this contract.
 *
 * <p>The cross-field rule (endDate strictly after startDate) is enforced by the
 * service rather than by a fragile field-level annotation.
 */
public record RentalRequestDto(

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotNull(message = "vehicleId is required")
        Long vehicleId,

        @NotNull(message = "customerId is required")
        Long customerId
) {
}

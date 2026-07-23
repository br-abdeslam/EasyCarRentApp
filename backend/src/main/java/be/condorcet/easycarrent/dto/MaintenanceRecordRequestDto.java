package be.condorcet.easycarrent.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload used to create a maintenance record for a vehicle.
 *
 * <p>The client supplies the target vehicle, a description, the period and the
 * cost. The status is backend-controlled (a new record starts {@code PLANNED}),
 * so it is not part of this contract. Cross-field date-order validation and the
 * availability/overlap rules are enforced by the service layer, not here.
 */
public record MaintenanceRecordRequestDto(

        @NotNull(message = "vehicleId is required")
        Long vehicleId,

        @NotBlank(message = "description is required")
        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotNull(message = "cost is required")
        @PositiveOrZero(message = "cost must be zero or positive")
        @Digits(integer = 10, fraction = 2, message = "cost must have at most 10 integer digits and 2 decimals")
        BigDecimal cost
) {
}

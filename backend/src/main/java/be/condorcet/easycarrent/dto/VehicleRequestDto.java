package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.validation.ManufacturingYear;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request payload used to create or update a {@code Vehicle}.
 *
 * <p>The status is intentionally not part of this contract: new vehicles default
 * to {@code AVAILABLE} and status transitions are handled by later rental logic,
 * not by the client.
 */
public record VehicleRequestDto(

        @NotBlank(message = "registrationNumber is required")
        @Size(max = 20, message = "registrationNumber must be at most 20 characters")
        String registrationNumber,

        @NotBlank(message = "brand is required")
        @Size(max = 60, message = "brand must be at most 60 characters")
        String brand,

        @NotBlank(message = "model is required")
        @Size(max = 60, message = "model must be at most 60 characters")
        String model,

        @ManufacturingYear
        Integer manufacturingYear,

        @Size(max = 40, message = "color must be at most 40 characters")
        String color,

        @NotNull(message = "dailyPrice is required")
        @Positive(message = "dailyPrice must be positive")
        BigDecimal dailyPrice,

        @PositiveOrZero(message = "mileage cannot be negative")
        Long mileage,

        @NotNull(message = "categoryId is required")
        Long categoryId
) {
}

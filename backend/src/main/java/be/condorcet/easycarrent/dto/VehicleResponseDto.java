package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.entity.VehicleStatus;
import java.math.BigDecimal;

/**
 * Response payload exposing a {@code Vehicle} to API consumers.
 *
 * <p>The owning category is flattened into {@code categoryId} and
 * {@code categoryName} to avoid serializing the bidirectional JPA relationship.
 */
public record VehicleResponseDto(
        Long id,
        String registrationNumber,
        String brand,
        String model,
        Integer manufacturingYear,
        String color,
        BigDecimal dailyPrice,
        Long mileage,
        VehicleStatus status,
        Long categoryId,
        String categoryName
) {
}

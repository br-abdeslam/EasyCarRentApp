package be.condorcet.easycarrent.dto;

/**
 * Response payload exposing a {@code VehicleCategory} to API consumers.
 */
public record VehicleCategoryResponseDto(
        Long id,
        String name,
        String description
) {
}

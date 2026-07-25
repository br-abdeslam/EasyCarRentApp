package be.condorcet.easycarrent.desktop.dto;

/**
 * Client-side representation of a vehicle category returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly: {@code id}, {@code name},
 * and {@code description}. It carries no entities, nested vehicles, or
 * credentials.</p>
 *
 * @param id          the category identifier
 * @param name        the category name
 * @param description the category description, which may be null
 */
public record VehicleCategoryResponseDto(
		Long id,
		String name,
		String description) {
}

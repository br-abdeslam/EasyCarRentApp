package be.condorcet.easycarrent.desktop.dto;

/**
 * Request payload sent to the backend to create or update a vehicle category.
 *
 * <p>Mirrors the backend request contract exactly: a required {@code name} and an
 * optional {@code description}. It contains no identifier (the id is carried in
 * the request path) and no credentials.</p>
 *
 * @param name        the category name (required by the backend)
 * @param description the category description (optional)
 */
public record VehicleCategoryRequestDto(
		String name,
		String description) {
}

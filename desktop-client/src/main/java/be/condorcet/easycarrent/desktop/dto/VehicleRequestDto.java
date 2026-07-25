package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;

/**
 * Request payload sent to the backend to create or update a vehicle.
 *
 * <p>Mirrors the backend request contract exactly. It carries no identifier (the
 * id is in the request path) and no status (the status is backend-managed and
 * defaults to {@code AVAILABLE}). The owning category is referenced by
 * {@code categoryId}. Monetary values use {@link BigDecimal}.</p>
 *
 * @param registrationNumber the registration number (required)
 * @param brand             the brand (required)
 * @param model             the model (required)
 * @param manufacturingYear the manufacturing year (optional)
 * @param color             the color (optional)
 * @param dailyPrice        the daily price (required, positive)
 * @param mileage           the mileage (optional)
 * @param categoryId        the owning category's id (required)
 */
public record VehicleRequestDto(
		String registrationNumber,
		String brand,
		String model,
		Integer manufacturingYear,
		String color,
		BigDecimal dailyPrice,
		Long mileage,
		Long categoryId) {
}

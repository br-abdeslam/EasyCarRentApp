package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;

/**
 * Client-side representation of a vehicle returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly. The owning category is
 * flattened into {@code categoryId} and {@code categoryName} (no nested category
 * object). Monetary values use {@link BigDecimal}. No rental or maintenance
 * collections and no credentials are included.</p>
 *
 * @param id                the vehicle identifier
 * @param registrationNumber the registration number
 * @param brand             the brand
 * @param model             the model
 * @param manufacturingYear the manufacturing year, which may be null
 * @param color             the color, which may be null
 * @param dailyPrice        the daily price
 * @param mileage           the mileage, which may be null
 * @param status            the backend-managed status
 * @param categoryId        the owning category's id
 * @param categoryName      the owning category's name
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
		String categoryName) {
}

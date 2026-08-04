package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client-side representation of a rental returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly. The referenced vehicle and
 * customer are flattened into a few safe identifying fields (no nested objects and
 * no sensitive customer data such as address, phone, email, or driving licence).
 * Dates are {@link LocalDate}, the creation timestamp is a {@link LocalDateTime},
 * and the backend-calculated total price is a {@link BigDecimal}. This response is
 * never used as a request body.</p>
 *
 * @param id                        the rental identifier
 * @param startDate                 the first rental day
 * @param endDate                   the last rental day (strictly after the start)
 * @param status                    the backend-managed lifecycle status
 * @param totalPrice                the backend-calculated total price
 * @param createdAt                 the creation timestamp
 * @param vehicleId                 the booked vehicle's id
 * @param vehicleRegistrationNumber the booked vehicle's registration number
 * @param vehicleBrand              the booked vehicle's brand
 * @param vehicleModel              the booked vehicle's model
 * @param customerId                the renting customer's id
 * @param customerFirstName         the renting customer's first name
 * @param customerLastName          the renting customer's last name
 */
public record RentalResponseDto(
		Long id,
		LocalDate startDate,
		LocalDate endDate,
		RentalStatus status,
		BigDecimal totalPrice,
		LocalDateTime createdAt,
		Long vehicleId,
		String vehicleRegistrationNumber,
		String vehicleBrand,
		String vehicleModel,
		Long customerId,
		String customerFirstName,
		String customerLastName) {
}

package be.condorcet.easycarrent.desktop.dto;

import java.time.LocalDate;

/**
 * Request payload used to create or update a rental.
 *
 * <p>Mirrors the backend request contract exactly: the client supplies only the
 * booking period and the referenced vehicle and customer identifiers. The
 * identifier, status, total price, and creation timestamp are all controlled by
 * the backend and are intentionally not part of this contract, so none of them is
 * ever serialized in a request.</p>
 *
 * @param startDate  the requested first rental day
 * @param endDate    the requested last rental day (must be after the start date)
 * @param vehicleId  the identifier of the vehicle to book
 * @param customerId the identifier of the renting customer
 */
public record RentalRequestDto(
		LocalDate startDate,
		LocalDate endDate,
		Long vehicleId,
		Long customerId) {
}

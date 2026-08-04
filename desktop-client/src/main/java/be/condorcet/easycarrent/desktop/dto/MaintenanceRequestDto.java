package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload used to create a maintenance record for a vehicle.
 *
 * <p>Mirrors the backend request contract exactly: the client supplies the target
 * vehicle, a description, the period, and the cost. The identifier and status are
 * controlled by the backend (a new record starts {@code PLANNED}), so neither is
 * ever serialized in a request.</p>
 *
 * @param vehicleId   the identifier of the vehicle to maintain
 * @param description the maintenance description (backend limit: 500 characters)
 * @param startDate   the requested first maintenance day
 * @param endDate     the requested last maintenance day (on or after the start)
 * @param cost        the maintenance cost (zero or positive)
 */
public record MaintenanceRequestDto(
		Long vehicleId,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal cost) {
}

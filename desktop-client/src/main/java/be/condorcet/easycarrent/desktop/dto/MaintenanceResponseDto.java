package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Client-side representation of a maintenance record returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly. The vehicle is referenced only
 * by its id (no nested vehicle, category, customer, or payment data). The period is
 * a pair of {@link LocalDate}s, the cost is a {@link BigDecimal}, and the status is
 * backend-managed. This response is never used as a request body.</p>
 *
 * @param id          the maintenance-record identifier
 * @param vehicleId   the vehicle under maintenance
 * @param description the maintenance description
 * @param startDate   the first maintenance day
 * @param endDate     the last maintenance day (on or after the start)
 * @param cost        the maintenance cost
 * @param status      the backend-managed lifecycle status
 */
public record MaintenanceResponseDto(
		Long id,
		Long vehicleId,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal cost,
		MaintenanceStatus status) {
}

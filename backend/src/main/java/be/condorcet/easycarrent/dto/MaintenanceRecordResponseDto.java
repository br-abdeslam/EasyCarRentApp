package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.entity.MaintenanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response payload exposing a maintenance record to API consumers.
 *
 * <p>Only the identifying vehicle reference and the maintenance details are
 * exposed. No entity, vehicle relationship, category, customer or payment data
 * is included.
 */
public record MaintenanceRecordResponseDto(
        Long id,
        Long vehicleId,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal cost,
        MaintenanceStatus status
) {
}

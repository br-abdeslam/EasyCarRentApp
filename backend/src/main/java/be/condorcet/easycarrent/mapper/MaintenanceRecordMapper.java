package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.Vehicle;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link MaintenanceRecord} entities and their DTOs.
 *
 * <p>The referenced {@link Vehicle} and the normalized cost are resolved by the
 * service layer and passed in; this mapper never queries repositories,
 * normalizes cost, validates dates, checks overlaps or makes business decisions.
 */
@Component
public class MaintenanceRecordMapper {

    /**
     * Builds a new maintenance record from the request, the resolved vehicle and
     * the normalized cost. The status defaults to {@code PLANNED} through the
     * entity constructor. The cost comes from the service layer, never
     * recalculated here.
     */
    public MaintenanceRecord toEntity(MaintenanceRecordRequestDto request, Vehicle vehicle,
                                      BigDecimal normalizedCost) {
        return new MaintenanceRecord(
                vehicle,
                request.description(),
                request.startDate(),
                request.endDate(),
                normalizedCost);
    }

    public MaintenanceRecordResponseDto toResponse(MaintenanceRecord record) {
        Vehicle vehicle = record.getVehicle();
        return new MaintenanceRecordResponseDto(
                record.getId(),
                vehicle != null ? vehicle.getId() : null,
                record.getDescription(),
                record.getStartDate(),
                record.getEndDate(),
                record.getCost(),
                record.getStatus());
    }
}

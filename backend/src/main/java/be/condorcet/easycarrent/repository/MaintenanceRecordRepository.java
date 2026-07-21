package be.condorcet.easycarrent.repository;

import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findAllByVehicle_IdOrderByStartDateAsc(Long vehicleId);

    List<MaintenanceRecord> findAllByStatusOrderByStartDateAsc(MaintenanceStatus status);

    /**
     * Tells whether the vehicle already has a maintenance record whose inclusive
     * date range overlaps the requested period and whose status is one of the
     * supplied blocking statuses. Two ranges overlap when
     * {@code existing.startDate <= candidateEndDate AND existing.endDate >= candidateStartDate}.
     *
     * <p>This means a record ending on the candidate start date, or starting on
     * the candidate end date, still overlaps; only completely separate periods do
     * not. The caller (service layer) decides which statuses block, typically
     * {@code PLANNED} and {@code IN_PROGRESS}. The repository makes no business
     * decision.
     */
    boolean existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long vehicleId,
            Collection<MaintenanceStatus> statuses,
            LocalDate candidateEndDate,
            LocalDate candidateStartDate);
}

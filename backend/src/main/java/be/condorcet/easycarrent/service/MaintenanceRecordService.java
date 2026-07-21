package be.condorcet.easycarrent.service;

import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.MaintenanceRecordMapper;
import be.condorcet.easycarrent.repository.MaintenanceRecordRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for maintenance records: reads and creation.
 *
 * <p>A maintenance record may be created for any vehicle that is not
 * {@link VehicleStatus#INACTIVE}, provided the requested period does not overlap
 * a blocking maintenance record ({@code PLANNED}/{@code IN_PROGRESS}) or a
 * blocking rental ({@code PLANNED}/{@code ACTIVE}) on the same vehicle. The cost
 * is normalized to scale 2 by the backend. Lifecycle transitions, vehicle-status
 * synchronization and deletion are handled by later steps and are not
 * implemented here.
 *
 * <p><strong>Concurrency (documented MVP limitation):</strong> vehicle
 * resolution, both overlap checks and the insert run in one write transaction,
 * but two concurrent requests could both pass the overlap checks before either
 * insert commits. No database exclusion constraint currently prevents
 * overlapping periods; adding one is out of scope for the MVP.
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceRecordService {

    /** Maintenance statuses that block a new overlapping maintenance period. */
    private static final Set<MaintenanceStatus> BLOCKING_MAINTENANCE_STATUSES =
            Set.of(MaintenanceStatus.PLANNED, MaintenanceStatus.IN_PROGRESS);

    /** Rental statuses that block a new overlapping maintenance period. */
    private static final Set<RentalStatus> BLOCKING_RENTAL_STATUSES =
            Set.of(RentalStatus.PLANNED, RentalStatus.ACTIVE);

    private static final int MONETARY_SCALE = 2;

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalRepository rentalRepository;
    private final MaintenanceRecordMapper maintenanceRecordMapper;

    public MaintenanceRecordService(MaintenanceRecordRepository maintenanceRecordRepository,
                                    VehicleRepository vehicleRepository,
                                    RentalRepository rentalRepository,
                                    MaintenanceRecordMapper maintenanceRecordMapper) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleRepository = vehicleRepository;
        this.rentalRepository = rentalRepository;
        this.maintenanceRecordMapper = maintenanceRecordMapper;
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    public List<MaintenanceRecordResponseDto> findAll() {
        return maintenanceRecordRepository.findAll().stream()
                .map(maintenanceRecordMapper::toResponse)
                .toList();
    }

    public MaintenanceRecordResponseDto findById(Long id) {
        return maintenanceRecordMapper.toResponse(getRecord(id));
    }

    /**
     * Returns the maintenance records of one vehicle, ordered by start date. The
     * vehicle is checked first so a missing vehicle produces a not-found error
     * rather than a silently empty list.
     */
    public List<MaintenanceRecordResponseDto> findByVehicleId(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found with id: " + vehicleId);
        }
        return maintenanceRecordRepository.findAllByVehicle_IdOrderByStartDateAsc(vehicleId).stream()
                .map(maintenanceRecordMapper::toResponse)
                .toList();
    }

    public List<MaintenanceRecordResponseDto> findByStatus(MaintenanceStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return maintenanceRecordRepository.findAllByStatusOrderByStartDateAsc(status).stream()
                .map(maintenanceRecordMapper::toResponse)
                .toList();
    }

    // ---------------------------------------------------------------------
    // Creation
    // ---------------------------------------------------------------------

    @Transactional
    public MaintenanceRecordResponseDto create(MaintenanceRecordRequestDto request) {
        validateRequest(request);
        validatePeriod(request.startDate(), request.endDate());
        validateCostNotNegative(request.cost());

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + request.vehicleId()));
        validateVehicleIsMaintainable(vehicle);

        rejectMaintenanceOverlap(vehicle.getId(), request.startDate(), request.endDate());
        rejectRentalOverlap(vehicle.getId(), request.startDate(), request.endDate());

        BigDecimal normalizedCost = request.cost().setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        MaintenanceRecord record = maintenanceRecordMapper.toEntity(request, vehicle, normalizedCost);
        return maintenanceRecordMapper.toResponse(maintenanceRecordRepository.save(record));
    }

    // ---------------------------------------------------------------------
    // Lifecycle transitions and deletion
    // ---------------------------------------------------------------------

    /**
     * Starts a planned maintenance: the record must be {@code PLANNED} and its
     * vehicle {@code AVAILABLE}. The record becomes {@code IN_PROGRESS} and the
     * vehicle {@code MAINTENANCE}; both changes are persisted in one transaction.
     */
    @Transactional
    public MaintenanceRecordResponseDto start(Long id) {
        MaintenanceRecord record = getRecord(id);
        requireRecordStatus(record, MaintenanceStatus.PLANNED, "started");
        Vehicle vehicle = record.getVehicle();
        requireVehicleStatus(record, vehicle, VehicleStatus.AVAILABLE, "started");

        record.start();
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        maintenanceRecordRepository.save(record);
        vehicleRepository.save(vehicle);
        return maintenanceRecordMapper.toResponse(record);
    }

    /**
     * Completes an in-progress maintenance: the record must be
     * {@code IN_PROGRESS} and its vehicle {@code MAINTENANCE}. The record becomes
     * {@code COMPLETED} and the vehicle {@code AVAILABLE}; both changes are
     * persisted in one transaction.
     */
    @Transactional
    public MaintenanceRecordResponseDto complete(Long id) {
        MaintenanceRecord record = getRecord(id);
        requireRecordStatus(record, MaintenanceStatus.IN_PROGRESS, "completed");
        Vehicle vehicle = record.getVehicle();
        requireVehicleStatus(record, vehicle, VehicleStatus.MAINTENANCE, "completed");

        record.complete();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        maintenanceRecordRepository.save(record);
        vehicleRepository.save(vehicle);
        return maintenanceRecordMapper.toResponse(record);
    }

    /**
     * Deletes a maintenance record only while it is {@code PLANNED}. In-progress
     * and completed records are kept as history. Deletion never touches the
     * vehicle.
     */
    @Transactional
    public void delete(Long id) {
        MaintenanceRecord record = getRecord(id);
        if (record.getStatus() != MaintenanceStatus.PLANNED) {
            throw new ResourceConflictException(
                    "Maintenance record " + id + " is " + record.getStatus()
                            + " and can only be deleted while PLANNED");
        }
        maintenanceRecordRepository.delete(record);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validateRequest(MaintenanceRecordRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Maintenance request must not be null");
        }
        if (request.vehicleId() == null) {
            throw new IllegalArgumentException("vehicleId must not be null");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (request.startDate() == null) {
            throw new IllegalArgumentException("startDate must not be null");
        }
        if (request.endDate() == null) {
            throw new IllegalArgumentException("endDate must not be null");
        }
        if (request.cost() == null) {
            throw new IllegalArgumentException("cost must not be null");
        }
    }

    /** Requires {@code startDate <= endDate}; same-day maintenance is allowed. */
    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate " + endDate + " must not be before startDate " + startDate);
        }
    }

    /** Requires {@code cost >= 0}; zero is allowed. */
    private void validateCostNotNegative(BigDecimal cost) {
        if (cost.signum() < 0) {
            throw new IllegalArgumentException("cost must be zero or positive but was " + cost);
        }
    }

    /**
     * Only an {@link VehicleStatus#INACTIVE} vehicle is rejected. AVAILABLE,
     * RENTED and MAINTENANCE describe the current operational state; temporal
     * conflicts for the requested period are handled by the overlap checks.
     */
    private void validateVehicleIsMaintainable(Vehicle vehicle) {
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new ResourceConflictException(
                    "Vehicle " + vehicle.getId() + " is INACTIVE and cannot have maintenance scheduled");
        }
    }

    private void rejectMaintenanceOverlap(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        boolean overlaps = maintenanceRecordRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicleId, BLOCKING_MAINTENANCE_STATUSES, endDate, startDate);
        if (overlaps) {
            throw new ResourceConflictException(
                    "Vehicle " + vehicleId + " already has maintenance scheduled overlapping "
                            + startDate + " to " + endDate);
        }
    }

    private void rejectRentalOverlap(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        long overlaps = rentalRepository.countOverlappingRentals(
                vehicleId, startDate, endDate, BLOCKING_RENTAL_STATUSES);
        if (overlaps > 0) {
            throw new ResourceConflictException(
                    "Vehicle " + vehicleId + " has a rental overlapping "
                            + startDate + " to " + endDate);
        }
    }

    private void requireRecordStatus(MaintenanceRecord record, MaintenanceStatus required, String action) {
        if (record.getStatus() != required) {
            throw new ResourceConflictException(
                    "Maintenance record " + record.getId() + " can only be " + action
                            + " from " + required + " but is " + record.getStatus());
        }
    }

    private void requireVehicleStatus(MaintenanceRecord record, Vehicle vehicle,
                                      VehicleStatus required, String action) {
        if (vehicle.getStatus() != required) {
            throw new ResourceConflictException(
                    "Maintenance record " + record.getId() + " cannot be " + action
                            + " because vehicle " + vehicle.getId() + " is " + vehicle.getStatus()
                            + " (must be " + required + ")");
        }
    }

    private MaintenanceRecord getRecord(Long id) {
        return maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Maintenance record not found with id: " + id));
    }
}

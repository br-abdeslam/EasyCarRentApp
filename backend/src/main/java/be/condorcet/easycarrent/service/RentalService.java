package be.condorcet.easycarrent.service;

import be.condorcet.easycarrent.dto.RentalRequestDto;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.InvalidRentalPeriodException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.RentalMapper;
import be.condorcet.easycarrent.mapper.VehicleMapper;
import be.condorcet.easycarrent.repository.CustomerRepository;
import be.condorcet.easycarrent.repository.MaintenanceRecordRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for rentals: booking rules, price calculation, date-overlap
 * rejection and lifecycle transitions.
 *
 * <p>The vehicle-availability lookup currently issues one overlap query per
 * candidate vehicle. This is acceptable for the course MVP but is a known
 * scalability limitation that a future issue could replace with a single set
 * based query.
 */
@Service
@Transactional(readOnly = true)
public class RentalService {

    /**
     * The non-empty set of statuses that block a vehicle for a period. The
     * overlap queries are never called with an empty collection.
     */
    private static final Set<RentalStatus> BLOCKING_STATUSES =
            Set.of(RentalStatus.PLANNED, RentalStatus.ACTIVE);

    /** Maintenance statuses that block a vehicle for a rental period. COMPLETED never blocks. */
    private static final Set<MaintenanceStatus> BLOCKING_MAINTENANCE_STATUSES =
            Set.of(MaintenanceStatus.PLANNED, MaintenanceStatus.IN_PROGRESS);

    /** Vehicle states that may be considered for a future booking. */
    private static final Set<VehicleStatus> BOOKABLE_STATUSES =
            Set.of(VehicleStatus.AVAILABLE, VehicleStatus.RENTED);

    private static final int MONETARY_SCALE = 2;

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final RentalMapper rentalMapper;
    private final VehicleMapper vehicleMapper;

    public RentalService(RentalRepository rentalRepository,
                         VehicleRepository vehicleRepository,
                         CustomerRepository customerRepository,
                         MaintenanceRecordRepository maintenanceRecordRepository,
                         RentalMapper rentalMapper,
                         VehicleMapper vehicleMapper) {
        this.rentalRepository = rentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.rentalMapper = rentalMapper;
        this.vehicleMapper = vehicleMapper;
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    public List<RentalResponseDto> findAll() {
        return rentalRepository.findAll().stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    public RentalResponseDto findById(Long id) {
        return rentalMapper.toResponse(getRental(id));
    }

    /**
     * Returns vehicles that are bookable (AVAILABLE or RENTED, never INACTIVE or
     * MAINTENANCE) and have no PLANNED or ACTIVE rental overlapping the period.
     */
    public List<VehicleResponseDto> findAvailableVehicles(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return vehicleRepository.findAll().stream()
                .filter(vehicle -> BOOKABLE_STATUSES.contains(vehicle.getStatus()))
                .filter(vehicle -> rentalRepository.countOverlappingRentals(
                        vehicle.getId(), startDate, endDate, BLOCKING_STATUSES) == 0)
                .map(vehicleMapper::toResponse)
                .toList();
    }

    // ---------------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------------

    @Transactional
    public RentalResponseDto create(RentalRequestDto request) {
        validatePeriod(request.startDate(), request.endDate());
        Customer customer = getCustomer(request.customerId());
        Vehicle vehicle = getVehicle(request.vehicleId());
        validateLicence(customer, request.endDate());
        validateVehicleBookable(vehicle);
        rejectOverlapForCreate(vehicle, request.startDate(), request.endDate());
        rejectMaintenanceOverlap(vehicle, request.startDate(), request.endDate());

        BigDecimal totalPrice = calculateTotalPrice(vehicle, request.startDate(), request.endDate());
        Rental rental = rentalMapper.toEntity(request, vehicle, customer, totalPrice);
        return rentalMapper.toResponse(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponseDto update(Long id, RentalRequestDto request) {
        Rental rental = getRental(id);
        if (rental.getStatus() != RentalStatus.PLANNED) {
            throw new ResourceConflictException(
                    "Only a PLANNED rental can be updated; rental " + id + " is " + rental.getStatus());
        }
        validatePeriod(request.startDate(), request.endDate());
        Customer customer = getCustomer(request.customerId());
        Vehicle vehicle = getVehicle(request.vehicleId());
        validateLicence(customer, request.endDate());
        validateVehicleBookable(vehicle);
        rejectOverlapForUpdate(vehicle, rental.getId(), request.startDate(), request.endDate());
        rejectMaintenanceOverlap(vehicle, request.startDate(), request.endDate());

        BigDecimal totalPrice = calculateTotalPrice(vehicle, request.startDate(), request.endDate());
        rentalMapper.updateEntity(rental, request, vehicle, customer, totalPrice);
        return rentalMapper.toResponse(rentalRepository.save(rental));
    }

    @Transactional
    public void delete(Long id) {
        Rental rental = getRental(id);
        RentalStatus status = rental.getStatus();
        if (status == RentalStatus.ACTIVE) {
            throw new ResourceConflictException("An active rental cannot be deleted");
        }
        if (status == RentalStatus.COMPLETED) {
            // Completed rentals are retained as history and cannot be deleted.
            throw new ResourceConflictException("A completed rental cannot be deleted");
        }
        rentalRepository.delete(rental);
    }

    // ---------------------------------------------------------------------
    // Lifecycle transitions
    // ---------------------------------------------------------------------

    @Transactional
    public RentalResponseDto start(Long id) {
        Rental rental = getRental(id);
        requireStatus(rental, RentalStatus.PLANNED, "started");
        Vehicle vehicle = rental.getVehicle();
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResourceConflictException(
                    "The vehicle must be AVAILABLE to start this rental but is " + vehicle.getStatus());
        }
        // Guard against another PLANNED/ACTIVE rental that would conflict at start time.
        rejectOverlapForUpdate(vehicle, rental.getId(), rental.getStartDate(), rental.getEndDate());

        rental.setStatus(RentalStatus.ACTIVE);
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicleRepository.save(vehicle);
        return rentalMapper.toResponse(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponseDto complete(Long id) {
        Rental rental = getRental(id);
        requireStatus(rental, RentalStatus.ACTIVE, "completed");
        rental.setStatus(RentalStatus.COMPLETED);
        Vehicle vehicle = rental.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.RENTED) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }
        return rentalMapper.toResponse(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponseDto cancel(Long id) {
        Rental rental = getRental(id);
        requireStatus(rental, RentalStatus.PLANNED, "cancelled");
        rental.setStatus(RentalStatus.CANCELLED);
        // Cancelling a PLANNED rental never changes the vehicle status.
        return rentalMapper.toResponse(rentalRepository.save(rental));
    }

    // ---------------------------------------------------------------------
    // Shared validation helpers
    // ---------------------------------------------------------------------

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidRentalPeriodException("startDate and endDate are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw new InvalidRentalPeriodException("endDate must be strictly after startDate");
        }
    }

    private void validateLicence(Customer customer, LocalDate endDate) {
        if (customer.getDrivingLicenseExpiryDate().isBefore(endDate)) {
            throw new ResourceConflictException(
                    "The customer driving licence expires before the rental end date");
        }
    }

    private void validateVehicleBookable(Vehicle vehicle) {
        VehicleStatus status = vehicle.getStatus();
        if (status == VehicleStatus.INACTIVE || status == VehicleStatus.MAINTENANCE) {
            throw new ResourceConflictException(
                    "The vehicle cannot be booked while it is " + status);
        }
    }

    private void rejectOverlapForCreate(Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        long overlaps = rentalRepository.countOverlappingRentals(
                vehicle.getId(), startDate, endDate, BLOCKING_STATUSES);
        if (overlaps > 0) {
            throw new ResourceConflictException(
                    "The vehicle already has a planned or active rental overlapping the requested period");
        }
    }

    private void rejectOverlapForUpdate(Vehicle vehicle, Long currentRentalId,
                                        LocalDate startDate, LocalDate endDate) {
        long overlaps = rentalRepository.countOverlappingRentalsExcludingRentalId(
                vehicle.getId(), currentRentalId, startDate, endDate, BLOCKING_STATUSES);
        if (overlaps > 0) {
            throw new ResourceConflictException(
                    "The vehicle already has a planned or active rental overlapping the requested period");
        }
    }

    /**
     * Rejects a rental period that overlaps a PLANNED or IN_PROGRESS maintenance
     * record for the same vehicle. Overlap is inclusive:
     * {@code maintenance.startDate <= rental.endDate AND maintenance.endDate >= rental.startDate}.
     * COMPLETED maintenance never blocks. No maintenance record is modified.
     */
    private void rejectMaintenanceOverlap(Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        boolean overlaps = maintenanceRecordRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicle.getId(), BLOCKING_MAINTENANCE_STATUSES, endDate, startDate);
        if (overlaps) {
            throw new ResourceConflictException(
                    "Vehicle " + vehicle.getId() + " has maintenance overlapping the requested period "
                            + startDate + " to " + endDate);
        }
    }

    private BigDecimal calculateTotalPrice(Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        long billableDays = ChronoUnit.DAYS.between(startDate, endDate);
        return vehicle.getDailyPrice()
                .multiply(BigDecimal.valueOf(billableDays))
                .setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireStatus(Rental rental, RentalStatus required, String action) {
        if (rental.getStatus() != required) {
            throw new ResourceConflictException(
                    "A rental can only be " + action + " from " + required
                            + " but rental " + rental.getId() + " is " + rental.getStatus());
        }
    }

    private Rental getRental(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found with id " + id));
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id " + id));
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}

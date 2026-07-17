package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.RentalRequestDto;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.Customer;
import be.condorcet.easycarrent.entity.Rental;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.InvalidRentalPeriodException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.RentalMapper;
import be.condorcet.easycarrent.mapper.VehicleMapper;
import be.condorcet.easycarrent.repository.CustomerRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 6, 10);
    private static final LocalDate END = LocalDate.of(2026, 6, 20); // 10 billable days
    private static final BigDecimal DAILY = new BigDecimal("50.00");

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Captor
    private ArgumentCaptor<Collection<RentalStatus>> statusCaptor;

    private RentalService service;

    @BeforeEach
    void setUp() {
        service = new RentalService(rentalRepository, vehicleRepository, customerRepository,
                new RentalMapper(), new VehicleMapper());
    }

    // ------------------------------------------------------------------ fixtures

    private Vehicle vehicle(Long id, VehicleStatus status) {
        VehicleCategory category = new VehicleCategory("Economy", null);
        ReflectionTestUtils.setField(category, "id", 7L);
        Vehicle vehicle = new Vehicle("1-ABC-" + id, "Toyota", "Corolla", 2022, "Blue",
                DAILY, 15000L, status, category);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    private Customer customer(Long id, LocalDate licenceExpiry) {
        Customer customer = new Customer("John", "Doe", "john" + id + "@example.com",
                "+32 470 12 34 56", "Rue de la Loi 16", "LIC-" + id, licenceExpiry);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private Rental rental(Long id, RentalStatus status, Vehicle vehicle, Customer customer,
                          LocalDate start, LocalDate end) {
        Rental rental = new Rental(vehicle, customer, start, end, new BigDecimal("500.00"), status);
        ReflectionTestUtils.setField(rental, "id", id);
        ReflectionTestUtils.setField(rental, "createdAt", LocalDateTime.of(2026, 1, 1, 12, 0));
        return rental;
    }

    private RentalRequestDto request(LocalDate start, LocalDate end, Long vehicleId, Long customerId) {
        return new RentalRequestDto(start, end, vehicleId, customerId);
    }

    private void stubSaveReturnsArgument() {
        when(rentalRepository.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================================================================== Read

    @Test
    void findAllReturnsMappedRentals() {
        Rental r = rental(1L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findAll()).thenReturn(List.of(r));

        List<RentalResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).vehicleId()).isEqualTo(1L);
        assertThat(result.get(0).customerFirstName()).isEqualTo("John");
    }

    @Test
    void findByIdReturnsRental() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        RentalResponseDto result = service.findById(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Create

    @Test
    void createPersistsPlannedRentalWithCalculatedPrice() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.create(request(START, END, 1L, 2L));

        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
        assertThat(result.totalPrice()).isEqualByComparingTo("500.00"); // 50.00 * 10 days
        assertThat(result.startDate()).isEqualTo(START);
        assertThat(result.endDate()).isEqualTo(END);
        assertThat(result.vehicleId()).isEqualTo(1L);
        assertThat(result.customerId()).isEqualTo(2L);
    }

    @Test
    void createCalculatesSingleBillableDay() {
        LocalDate end = START.plusDays(1); // exactly one billable day
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, end.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.create(request(START, end, 1L, 2L));

        assertThat(result.totalPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void createRejectsMissingCustomer() {
        when(customerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingVehicle() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRejectsEndDateEqualToStartDate() {
        assertThatThrownBy(() -> service.create(request(START, START, 1L, 2L)))
                .isInstanceOf(InvalidRentalPeriodException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> service.create(request(END, START, 1L, 2L)))
                .isInstanceOf(InvalidRentalPeriodException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRejectsExpiredLicence() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.minusDays(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createAcceptsLicenceExpiringExactlyOnEndDate() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END)));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.create(request(START, END, 1L, 2L));

        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
    }

    @Test
    void createRejectsInactiveVehicle() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.INACTIVE)));

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createRejectsMaintenanceVehicle() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.MAINTENANCE)));

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createAcceptsAvailableVehicle() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.create(request(START, END, 1L, 2L));

        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
    }

    @Test
    void createAcceptsRentedVehicleForNonOverlappingPeriod() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.RENTED)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.create(request(START, END, 1L, 2L));

        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
    }

    @Test
    void createRejectsOverlapWithPlannedOrActiveRental() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void createPassesExactlyPlannedAndActiveAsBlockingStatuses() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), statusCaptor.capture())).thenReturn(0L);
        stubSaveReturnsArgument();

        service.create(request(START, END, 1L, 2L));

        Collection<RentalStatus> blocking = statusCaptor.getValue();
        assertThat(blocking).isNotEmpty()
                .containsExactlyInAnyOrder(RentalStatus.PLANNED, RentalStatus.ACTIVE);
    }

    // ==================================================================== Update

    @Test
    void updateAppliesChangesAndRecalculatesPrice() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(2))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(eq(1L), eq(5L), any(), any(), any()))
                .thenReturn(0L);
        stubSaveReturnsArgument();

        LocalDate newEnd = START.plusDays(4); // 4 billable days -> 200.00
        RentalResponseDto result = service.update(5L, request(START, newEnd, 1L, 2L));

        assertThat(result.totalPrice()).isEqualByComparingTo("200.00");
        assertThat(result.endDate()).isEqualTo(newEnd);
    }

    @Test
    void updatePreservesStatusAndCreatedAt() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        LocalDateTime originalCreatedAt = existing.getCreatedAt();
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(eq(1L), eq(5L), any(), any(), any()))
                .thenReturn(0L);
        stubSaveReturnsArgument();

        RentalResponseDto result = service.update(5L, request(START, END, 1L, 2L));

        assertThat(result.status()).isEqualTo(RentalStatus.PLANNED);
        assertThat(result.createdAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void updateExcludesCurrentRentalUsingItsRealNonNullId() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(
                eq(1L), idCaptor.capture(), any(), any(), any())).thenReturn(0L);
        stubSaveReturnsArgument();

        service.update(5L, request(START, END, 1L, 2L));

        assertThat(idCaptor.getValue()).isNotNull().isEqualTo(5L);
    }

    @Test
    void updateRejectsAnotherOverlappingRental() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(eq(1L), eq(5L), any(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenRentalMissing() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateThrowsWhenNewCustomerMissing() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateThrowsWhenNewVehicleMissing() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsActiveRental() {
        Rental existing = rental(5L, RentalStatus.ACTIVE, vehicle(1L, VehicleStatus.RENTED),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateRejectsCompletedRental() {
        Rental existing = rental(5L, RentalStatus.COMPLETED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateRejectsCancelledRental() {
        Rental existing = rental(5L, RentalStatus.CANCELLED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidDates() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(5L, request(END, START, 1L, 2L)))
                .isInstanceOf(InvalidRentalPeriodException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidLicence() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.minusDays(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.AVAILABLE)));

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidVehicleState() {
        Rental existing = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer(2L, END.plusYears(1))));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, VehicleStatus.MAINTENANCE)));

        assertThatThrownBy(() -> service.update(5L, request(START, END, 1L, 2L)))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    // ==================================================================== Delete

    @Test
    void deletePlannedRentalSucceeds() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        service.delete(5L);

        verify(rentalRepository).delete(r);
    }

    @Test
    void deleteCancelledRentalSucceeds() {
        Rental r = rental(5L, RentalStatus.CANCELLED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        service.delete(5L);

        verify(rentalRepository).delete(r);
    }

    @Test
    void deleteActiveRentalRejected() {
        Rental r = rental(5L, RentalStatus.ACTIVE, vehicle(1L, VehicleStatus.RENTED),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void deleteCompletedRentalRejected() {
        Rental r = rental(5L, RentalStatus.COMPLETED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================================================================== Start

    @Test
    void startTransitionsPlannedToActiveAndRentsVehicle() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE);
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle, customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(eq(1L), eq(5L), any(), any(), any()))
                .thenReturn(0L);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSaveReturnsArgument();

        RentalResponseDto result = service.start(5L);

        assertThat(result.status()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RENTED);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void startRejectsActiveRental() {
        Rental r = rental(5L, RentalStatus.ACTIVE, vehicle(1L, VehicleStatus.RENTED),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsCompletedRental() {
        Rental r = rental(5L, RentalStatus.COMPLETED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsCancelledRental() {
        Rental r = rental(5L, RentalStatus.CANCELLED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsRentedVehicle() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.RENTED),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsMaintenanceVehicle() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.MAINTENANCE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsInactiveVehicle() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.INACTIVE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void startRejectsWhenAnotherConflictingRentalIsRechecked() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));
        when(rentalRepository.countOverlappingRentalsExcludingRentalId(eq(1L), eq(5L), any(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.start(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    // ==================================================================== Complete

    @Test
    void completeTransitionsActiveToCompletedAndFreesVehicle() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.RENTED);
        Rental r = rental(5L, RentalStatus.ACTIVE, vehicle, customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSaveReturnsArgument();

        RentalResponseDto result = service.complete(5L);

        assertThat(result.status()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void completeRejectsPlannedRental() {
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.complete(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void completeRejectsCompletedRental() {
        Rental r = rental(5L, RentalStatus.COMPLETED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.complete(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void completeRejectsCancelledRental() {
        Rental r = rental(5L, RentalStatus.CANCELLED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.complete(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    // ==================================================================== Cancel

    @Test
    void cancelTransitionsPlannedToCancelledWithoutChangingVehicle() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE);
        Rental r = rental(5L, RentalStatus.PLANNED, vehicle, customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));
        stubSaveReturnsArgument();

        RentalResponseDto result = service.cancel(5L);

        assertThat(result.status()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void cancelRejectsActiveRental() {
        Rental r = rental(5L, RentalStatus.ACTIVE, vehicle(1L, VehicleStatus.RENTED),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void cancelRejectsCompletedRental() {
        Rental r = rental(5L, RentalStatus.COMPLETED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void cancelRejectsCancelledRental() {
        Rental r = rental(5L, RentalStatus.CANCELLED, vehicle(1L, VehicleStatus.AVAILABLE),
                customer(2L, END.plusYears(1)), START, END);
        when(rentalRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(ResourceConflictException.class);
        verify(rentalRepository, never()).save(any());
    }

    // ==================================================================== Availability

    @Test
    void availabilityRejectsInvalidPeriod() {
        assertThatThrownBy(() -> service.findAvailableVehicles(END, START))
                .isInstanceOf(InvalidRentalPeriodException.class);
        verify(vehicleRepository, never()).findAll();
    }

    @Test
    void availabilityReturnsAvailableNonOverlappingVehicle() {
        Vehicle available = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(available));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).extracting(VehicleResponseDto::id).containsExactly(1L);
    }

    @Test
    void availabilityReturnsRentedFutureNonOverlappingVehicle() {
        Vehicle rented = vehicle(1L, VehicleStatus.RENTED);
        when(vehicleRepository.findAll()).thenReturn(List.of(rented));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(0L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).extracting(VehicleResponseDto::id).containsExactly(1L);
    }

    @Test
    void availabilityExcludesOverlappingVehicle() {
        Vehicle available = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(available));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), any())).thenReturn(2L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).isEmpty();
    }

    @Test
    void availabilityExcludesInactiveVehicle() {
        Vehicle inactive = vehicle(1L, VehicleStatus.INACTIVE);
        Vehicle available = vehicle(2L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(inactive, available));
        when(rentalRepository.countOverlappingRentals(eq(2L), any(), any(), any())).thenReturn(0L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).extracting(VehicleResponseDto::id).containsExactly(2L);
    }

    @Test
    void availabilityExcludesMaintenanceVehicle() {
        Vehicle maintenance = vehicle(1L, VehicleStatus.MAINTENANCE);
        Vehicle available = vehicle(2L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(maintenance, available));
        when(rentalRepository.countOverlappingRentals(eq(2L), any(), any(), any())).thenReturn(0L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).extracting(VehicleResponseDto::id).containsExactly(2L);
    }

    @Test
    void availabilityUsesExactlyPlannedAndActiveBlockingStatuses() {
        Vehicle available = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(available));
        when(rentalRepository.countOverlappingRentals(eq(1L), any(), any(), statusCaptor.capture()))
                .thenReturn(0L);

        service.findAvailableVehicles(START, END);

        assertThat(statusCaptor.getValue()).isNotEmpty()
                .containsExactlyInAnyOrder(RentalStatus.PLANNED, RentalStatus.ACTIVE);
    }

    @Test
    void availabilityReturnsDtosNotEntities() {
        Vehicle available = vehicle(1L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findAll()).thenReturn(List.of(available));
        when(rentalRepository.countOverlappingRentals(anyLong(), any(), any(), any())).thenReturn(0L);

        List<VehicleResponseDto> result = service.findAvailableVehicles(START, END);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(VehicleResponseDto.class);
        assertThat(result.get(0).registrationNumber()).isEqualTo("1-ABC-1");
    }
}

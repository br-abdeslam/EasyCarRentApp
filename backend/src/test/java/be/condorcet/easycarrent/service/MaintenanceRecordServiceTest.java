package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.RentalStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.InvalidRequestException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.MaintenanceRecordMapper;
import be.condorcet.easycarrent.repository.MaintenanceRecordRepository;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 6, 10);
    private static final LocalDate END = LocalDate.of(2026, 6, 20);

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RentalRepository rentalRepository;

    private MaintenanceRecordService service;

    @BeforeEach
    void setUp() {
        // real mapper, consistent with the other service tests
        service = new MaintenanceRecordService(
                maintenanceRecordRepository, vehicleRepository, rentalRepository, new MaintenanceRecordMapper());
    }

    // ------------------------------------------------------------------ fixtures

    private Vehicle vehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle("REG-" + id, "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, status, null);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    private MaintenanceRecord record(Long id, Vehicle vehicle, String description,
                                     LocalDate start, LocalDate end, String cost, MaintenanceStatus status) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, description, start, end, new BigDecimal(cost));
        ReflectionTestUtils.setField(record, "id", id);
        if (status != null) {
            ReflectionTestUtils.setField(record, "status", status);
        }
        return record;
    }

    private MaintenanceRecordRequestDto request(Long vehicleId, String description,
                                                LocalDate start, LocalDate end, String cost) {
        return new MaintenanceRecordRequestDto(vehicleId, description, start, end, new BigDecimal(cost));
    }

    private MaintenanceRecordRequestDto validRequest(Long vehicleId) {
        return request(vehicleId, "Full brake service", START, END, "120.00");
    }

    private void stubSaveAssigningId(Long id) {
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenAnswer(inv -> {
            MaintenanceRecord r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", id);
            return r;
        });
    }

    /** Stubs a clean creation path: resolvable vehicle, no maintenance overlap, no rental overlap, save assigns id. */
    private void stubHappyPath(Vehicle vehicle, Long savedId) {
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(vehicle.getId()), anyCollection(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(rentalRepository.countOverlappingRentals(
                eq(vehicle.getId()), any(LocalDate.class), any(LocalDate.class), anyCollection()))
                .thenReturn(0L);
        stubSaveAssigningId(savedId);
    }

    private void verifyNeverSaved() {
        verify(maintenanceRecordRepository, never()).save(any(MaintenanceRecord.class));
    }

    // ==================================================================== Reads

    @Test
    void findAllReturnsMappedRecords() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(maintenanceRecordRepository.findAll()).thenReturn(List.of(
                record(1L, vehicle, "A", START, END, "10.00", null),
                record(2L, vehicle, "B", START, END, "20.00", null)));

        assertThat(service.findAll()).extracting(MaintenanceRecordResponseDto::id).containsExactly(1L, 2L);
    }

    @Test
    void findAllReturnsEmptyListWhenNone() {
        when(maintenanceRecordRepository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findByIdReturnsRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(maintenanceRecordRepository.findById(1L))
                .thenReturn(Optional.of(record(1L, vehicle, "A", START, END, "10.00", null)));

        MaintenanceRecordResponseDto result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.vehicleId()).isEqualTo(7L);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(maintenanceRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByVehicleIdReturnsOrderedMappedRecords() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.existsById(7L)).thenReturn(true);
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByStartDateAsc(7L)).thenReturn(List.of(
                record(1L, vehicle, "Earlier", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "10.00", null),
                record(2L, vehicle, "Later", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12), "20.00", null)));

        List<MaintenanceRecordResponseDto> result = service.findByVehicleId(7L);

        assertThat(result).extracting(MaintenanceRecordResponseDto::id).containsExactly(1L, 2L);
    }

    @Test
    void findByVehicleIdReturnsEmptyWhenVehicleHasNoRecords() {
        when(vehicleRepository.existsById(7L)).thenReturn(true);
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByStartDateAsc(7L)).thenReturn(List.of());

        assertThat(service.findByVehicleId(7L)).isEmpty();
    }

    @Test
    void findByVehicleIdThrowsWhenVehicleMissingAndDoesNotQueryRecords() {
        when(vehicleRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByVehicleId(7L)).isInstanceOf(ResourceNotFoundException.class);
        verify(maintenanceRecordRepository, never()).findAllByVehicle_IdOrderByStartDateAsc(anyLong());
    }

    @Test
    void findByStatusReturnsOrderedMappedRecords() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(maintenanceRecordRepository.findAllByStatusOrderByStartDateAsc(MaintenanceStatus.PLANNED)).thenReturn(List.of(
                record(1L, vehicle, "A", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "10.00", null),
                record(2L, vehicle, "B", LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 6), "20.00", null)));

        List<MaintenanceRecordResponseDto> result = service.findByStatus(MaintenanceStatus.PLANNED);

        assertThat(result).extracting(MaintenanceRecordResponseDto::id).containsExactly(1L, 2L);
    }

    @Test
    void findByStatusReturnsEmptyWhenNoRecords() {
        when(maintenanceRecordRepository.findAllByStatusOrderByStartDateAsc(MaintenanceStatus.COMPLETED))
                .thenReturn(List.of());

        assertThat(service.findByStatus(MaintenanceStatus.COMPLETED)).isEmpty();
    }

    @Test
    void findByStatusRejectsNull() {
        assertThatThrownBy(() -> service.findByStatus(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ==================================================================== Successful creation

    @Test
    void createWithAvailableVehiclePersistsPlannedRecordFromRequest() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        MaintenanceRecordResponseDto response = service.create(validRequest(7L));

        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository, times(1)).save(captor.capture());
        MaintenanceRecord saved = captor.getValue();
        assertThat(saved.getVehicle()).isSameAs(vehicle);
        assertThat(saved.getDescription()).isEqualTo("Full brake service");
        assertThat(saved.getStartDate()).isEqualTo(START);
        assertThat(saved.getEndDate()).isEqualTo(END);
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.vehicleId()).isEqualTo(7L);
        assertThat(response.description()).isEqualTo("Full brake service");
        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(END);
        assertThat(response.status()).isEqualTo(MaintenanceStatus.PLANNED);
    }

    @Test
    void createNormalizesCostToScaleTwoHalfUp() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        MaintenanceRecordResponseDto response = service.create(request(7L, "Work", START, END, "99.999"));

        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getCost()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getCost().scale()).isEqualTo(2);
        assertThat(response.cost()).isEqualByComparingTo("100.00");
    }

    @Test
    void createAllowsZeroCost() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        MaintenanceRecordResponseDto response = service.create(request(7L, "Work", START, END, "0"));

        assertThat(response.cost()).isEqualByComparingTo("0.00");
    }

    @Test
    void createAllowsSameDayMaintenance() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        MaintenanceRecordResponseDto response = service.create(request(7L, "Work", START, START, "10.00"));

        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(START);
    }

    @Test
    void createAllowsRentedVehicleWhenNoOverlap() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.RENTED);
        stubHappyPath(vehicle, 100L);

        assertThat(service.create(validRequest(7L)).id()).isEqualTo(100L);
    }

    @Test
    void createAllowsMaintenanceVehicleWhenNoOverlap() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        stubHappyPath(vehicle, 100L);

        assertThat(service.create(validRequest(7L)).id()).isEqualTo(100L);
    }

    @Test
    void createDoesNotModifyVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        service.create(validRequest(7L));

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createPassesExactBlockingStatusesAndBoundariesToOverlapQueries() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        stubHappyPath(vehicle, 100L);

        service.create(validRequest(7L));

        // Maintenance overlap: vehicleId, blocking={PLANNED,IN_PROGRESS},
        // request endDate as the <= boundary, request startDate as the >= boundary.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MaintenanceStatus>> maintenanceStatuses = ArgumentCaptor.forClass(Collection.class);
        verify(maintenanceRecordRepository)
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(7L), maintenanceStatuses.capture(), eq(END), eq(START));
        assertThat(maintenanceStatuses.getValue())
                .containsExactlyInAnyOrder(MaintenanceStatus.PLANNED, MaintenanceStatus.IN_PROGRESS)
                .doesNotContain(MaintenanceStatus.COMPLETED);

        // Rental overlap: vehicleId, requestedStart=startDate, requestedEnd=endDate, blocking={PLANNED,ACTIVE}.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<RentalStatus>> rentalStatuses = ArgumentCaptor.forClass(Collection.class);
        verify(rentalRepository).countOverlappingRentals(eq(7L), eq(START), eq(END), rentalStatuses.capture());
        assertThat(rentalStatuses.getValue())
                .containsExactlyInAnyOrder(RentalStatus.PLANNED, RentalStatus.ACTIVE)
                .doesNotContain(RentalStatus.COMPLETED, RentalStatus.CANCELLED);
    }

    // ==================================================================== Rejections

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsNullVehicleId() {
        assertThatThrownBy(() -> service.create(request(null, "Work", START, END, "10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsBlankDescription() {
        assertThatThrownBy(() -> service.create(request(7L, "   ", START, END, "10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsNullStartDate() {
        assertThatThrownBy(() -> service.create(request(7L, "Work", null, END, "10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsNullEndDate() {
        assertThatThrownBy(() -> service.create(request(7L, "Work", START, null, "10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsNullCost() {
        assertThatThrownBy(() -> service.create(new MaintenanceRecordRequestDto(7L, "Work", START, END, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsEndDateBeforeStartDateAsInvalidRequest() {
        assertThatThrownBy(() -> service.create(
                request(7L, "Work", LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 10), "10.00")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("end date").hasMessageContaining("start date");

        // The date-order check runs before any resolution, overlap or persistence.
        verify(vehicleRepository, never()).findById(anyLong());
        verify(maintenanceRecordRepository, never())
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        anyLong(), anyCollection(), any(), any());
        verify(rentalRepository, never()).countOverlappingRentals(anyLong(), any(), any(), anyCollection());
        verifyNeverSaved();
    }

    @Test
    void createRejectsNegativeCost() {
        assertThatThrownBy(() -> service.create(request(7L, "Work", START, END, "-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNeverSaved();
    }

    @Test
    void createThrowsWhenVehicleMissing() {
        when(vehicleRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validRequest(7L))).isInstanceOf(ResourceNotFoundException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsInactiveVehicle() {
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle(7L, VehicleStatus.INACTIVE)));

        assertThatThrownBy(() -> service.create(validRequest(7L))).isInstanceOf(ResourceConflictException.class);
        verifyNeverSaved();
    }

    @Test
    void createRejectsBlockingMaintenanceOverlapWithoutCheckingRental() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(7L), anyCollection(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest(7L))).isInstanceOf(ResourceConflictException.class);

        verify(rentalRepository, never()).countOverlappingRentals(anyLong(), any(), any(), anyCollection());
        verifyNeverSaved();
    }

    @Test
    void createRejectsBlockingRentalOverlap() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq(7L), anyCollection(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(rentalRepository.countOverlappingRentals(
                eq(7L), any(LocalDate.class), any(LocalDate.class), anyCollection()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.create(validRequest(7L))).isInstanceOf(ResourceConflictException.class);
        verifyNeverSaved();
    }

    // ==================================================================== Lifecycle: start

    private void assertNeitherSaved() {
        verify(maintenanceRecordRepository, never()).save(any(MaintenanceRecord.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void startTransitionsRecordAndVehicleAndReturnsInProgress() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        MaintenanceRecordResponseDto response = service.start(1L);

        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        verify(maintenanceRecordRepository, times(1)).save(record);
        verify(vehicleRepository, times(1)).save(vehicle);
        assertThat(response.status()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(response.vehicleId()).isEqualTo(7L);
        assertThat(vehicle.getRegistrationNumber()).isEqualTo("REG-7");
    }

    @Test
    void startRejectsInProgressRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PLANNED").hasMessageContaining("IN_PROGRESS");
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void startRejectsCompletedRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.COMPLETED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    void startRejectsRentedVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.RENTED);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.start(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("1").hasMessageContaining("7").hasMessageContaining("RENTED");
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RENTED);
    }

    @Test
    void startRejectsMaintenanceVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void startRejectsInactiveVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.INACTIVE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
    }

    @Test
    void startThrowsWhenRecordMissing() {
        when(maintenanceRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertNeitherSaved();
    }

    // ==================================================================== Lifecycle: complete

    @Test
    void completeTransitionsRecordAndVehicleAndReturnsCompleted() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        MaintenanceRecordResponseDto response = service.complete(1L);

        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(maintenanceRecordRepository, times(1)).save(record);
        verify(vehicleRepository, times(1)).save(vehicle);
        assertThat(response.status()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(response.vehicleId()).isEqualTo(7L);
        assertThat(vehicle.getRegistrationNumber()).isEqualTo("REG-7");
    }

    @Test
    void completeRejectsPlannedRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.complete(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("IN_PROGRESS").hasMessageContaining("PLANNED");
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void completeRejectsCompletedRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.COMPLETED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    void completeRejectsAvailableVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.complete(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("1").hasMessageContaining("7").hasMessageContaining("AVAILABLE");
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void completeRejectsRentedVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.RENTED);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RENTED);
    }

    @Test
    void completeRejectsInactiveVehicle() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.INACTIVE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(ResourceConflictException.class);
        assertNeitherSaved();
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
    }

    @Test
    void completeThrowsWhenRecordMissing() {
        when(maintenanceRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertNeitherSaved();
    }

    // ==================================================================== Deletion

    @Test
    void deletePlannedRecordSucceedsAndLeavesVehicleUnchanged() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.PLANNED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        service.delete(1L);

        verify(maintenanceRecordRepository, times(1)).delete(record);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    @Test
    void deleteRejectsInProgressRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.MAINTENANCE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PLANNED").hasMessageContaining("IN_PROGRESS");
        verify(maintenanceRecordRepository, never()).delete(any(MaintenanceRecord.class));
    }

    @Test
    void deleteRejectsCompletedRecord() {
        Vehicle vehicle = vehicle(7L, VehicleStatus.AVAILABLE);
        MaintenanceRecord record = record(1L, vehicle, "Work", START, END, "100.00", MaintenanceStatus.COMPLETED);
        when(maintenanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceConflictException.class);
        verify(maintenanceRecordRepository, never()).delete(any(MaintenanceRecord.class));
    }

    @Test
    void deleteThrowsWhenRecordMissing() {
        when(maintenanceRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(maintenanceRecordRepository, never()).delete(any(MaintenanceRecord.class));
    }
}

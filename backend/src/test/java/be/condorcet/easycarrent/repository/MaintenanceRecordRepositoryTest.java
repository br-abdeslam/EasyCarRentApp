package be.condorcet.easycarrent.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class MaintenanceRecordRepositoryTest {

    /** Statuses the service layer will later treat as blocking. */
    private static final Collection<MaintenanceStatus> BLOCKING =
            EnumSet.of(MaintenanceStatus.PLANNED, MaintenanceStatus.IN_PROGRESS);

    private static final LocalDate EXISTING_START = LocalDate.of(2026, 6, 10);
    private static final LocalDate EXISTING_END = LocalDate.of(2026, 6, 20);

    @Autowired
    private MaintenanceRecordRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** Distinct suffix so unique columns (registration, category name) never clash. */
    private int sequence = 0;

    private Vehicle persistVehicle() {
        sequence++;
        VehicleCategory category = categoryRepository.saveAndFlush(new VehicleCategory("Cat-" + sequence, null));
        return vehicleRepository.saveAndFlush(new Vehicle(
                "REG-" + sequence, "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, category));
    }

    /** Builds a record, optionally overriding the default PLANNED status for arrangement. */
    private MaintenanceRecord newRecord(Vehicle vehicle, LocalDate start, LocalDate end,
                                        String cost, MaintenanceStatus status) {
        MaintenanceRecord record = new MaintenanceRecord(
                vehicle, "Maintenance " + start, start, end, new BigDecimal(cost));
        if (status != null) {
            // The entity intentionally has no status setter yet; the service will
            // own status transitions in a later step. Reflection only arranges the
            // persistence state for these repository tests.
            ReflectionTestUtils.setField(record, "status", status);
        }
        return record;
    }

    private MaintenanceRecord persistRecord(Vehicle vehicle, LocalDate start, LocalDate end,
                                            String cost, MaintenanceStatus status) {
        return maintenanceRepository.saveAndFlush(newRecord(vehicle, start, end, cost, status));
    }

    /** Persists one existing maintenance record on a fresh vehicle and returns that vehicle. */
    private Vehicle vehicleWithRecord(LocalDate start, LocalDate end, MaintenanceStatus status) {
        Vehicle vehicle = persistVehicle();
        persistRecord(vehicle, start, end, "100.00", status);
        return vehicle;
    }

    /** Overlap lookup expressed in natural (start, end) order to avoid argument confusion. */
    private boolean overlaps(Long vehicleId, LocalDate candidateStart, LocalDate candidateEnd,
                             Collection<MaintenanceStatus> statuses) {
        return maintenanceRepository
                .existsByVehicle_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicleId, statuses, candidateEnd, candidateStart);
    }

    // ---------------------------------------------------------------------
    // Persistence and relationship
    // ---------------------------------------------------------------------

    @Test
    void persistsAndReloadsRecordWithAllFields() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord saved = maintenanceRepository.saveAndFlush(new MaintenanceRecord(
                vehicle, "Full brake service", EXISTING_START, EXISTING_END, new BigDecimal("1234.5")));

        entityManager.clear();

        MaintenanceRecord reloaded = maintenanceRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("Full brake service");
        assertThat(reloaded.getStartDate()).isEqualTo(EXISTING_START);
        assertThat(reloaded.getEndDate()).isEqualTo(EXISTING_END);
        assertThat(reloaded.getCost()).isEqualByComparingTo("1234.50");
        assertThat(reloaded.getCost().scale()).isEqualTo(2);
        assertThat(reloaded.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
    }

    @Test
    void persistsVehicleRelationship() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord saved = persistRecord(vehicle, EXISTING_START, EXISTING_END, "100.00", null);

        entityManager.clear();

        MaintenanceRecord reloaded = maintenanceRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVehicle().getId()).isEqualTo(vehicle.getId());
    }

    @Test
    void persistsNonDefaultStatusAsEnum() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord saved = persistRecord(
                vehicle, EXISTING_START, EXISTING_END, "200.00", MaintenanceStatus.IN_PROGRESS);

        entityManager.clear();

        MaintenanceRecord reloaded = maintenanceRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
    }

    @Test
    void allowsMultipleRecordsForSameVehicle() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord first = persistRecord(vehicle, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), "50.00", null);
        MaintenanceRecord second = persistRecord(vehicle, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "60.00", null);

        entityManager.clear();

        List<MaintenanceRecord> records = maintenanceRepository.findAllByVehicle_IdOrderByStartDateAsc(vehicle.getId());
        assertThat(records).extracting(MaintenanceRecord::getId)
                .containsExactly(first.getId(), second.getId());
    }

    // ---------------------------------------------------------------------
    // Required-column and relationship constraints
    // ---------------------------------------------------------------------

    @Test
    void rejectsMissingVehicle() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                null, "Work", EXISTING_START, EXISTING_END, new BigDecimal("100.00"));

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullDescription() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                persistVehicle(), null, EXISTING_START, EXISTING_END, new BigDecimal("100.00"));

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullStartDate() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                persistVehicle(), "Work", null, EXISTING_END, new BigDecimal("100.00"));

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullEndDate() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                persistVehicle(), "Work", EXISTING_START, null, new BigDecimal("100.00"));

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullCost() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                persistVehicle(), "Work", EXISTING_START, EXISTING_END, null);

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullStatus() {
        MaintenanceRecord invalid = new MaintenanceRecord(
                persistVehicle(), "Work", EXISTING_START, EXISTING_END, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(invalid, "status", null);

        assertThatThrownBy(() -> maintenanceRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------
    // findAllByVehicle_IdOrderByStartDateAsc
    // ---------------------------------------------------------------------

    @Test
    void findAllByVehicleReturnsOnlyThatVehicleOrderedByStartDate() {
        Vehicle vehicle = persistVehicle();
        Vehicle otherVehicle = persistVehicle();
        MaintenanceRecord later = persistRecord(vehicle, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), "50.00", null);
        MaintenanceRecord earlier = persistRecord(vehicle, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), "60.00", null);
        persistRecord(otherVehicle, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6), "70.00", null);

        List<MaintenanceRecord> result = maintenanceRepository.findAllByVehicle_IdOrderByStartDateAsc(vehicle.getId());

        assertThat(result).extracting(MaintenanceRecord::getId)
                .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void findAllByVehicleReturnsEmptyForUnknownVehicle() {
        persistRecord(persistVehicle(), EXISTING_START, EXISTING_END, "100.00", null);

        assertThat(maintenanceRepository.findAllByVehicle_IdOrderByStartDateAsc(-1L)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // findAllByStatusOrderByStartDateAsc
    // ---------------------------------------------------------------------

    @Test
    void findAllByStatusReturnsOnlyThatStatusOrderedByStartDate() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord ipLater = persistRecord(vehicle, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), "50.00", MaintenanceStatus.IN_PROGRESS);
        MaintenanceRecord ipEarlier = persistRecord(vehicle, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), "60.00", MaintenanceStatus.IN_PROGRESS);
        persistRecord(vehicle, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6), "70.00", MaintenanceStatus.PLANNED);

        List<MaintenanceRecord> result = maintenanceRepository.findAllByStatusOrderByStartDateAsc(MaintenanceStatus.IN_PROGRESS);

        assertThat(result).extracting(MaintenanceRecord::getId)
                .containsExactly(ipEarlier.getId(), ipLater.getId());
    }

    @Test
    void findAllByStatusReturnsEmptyForStatusWithNoRecords() {
        persistRecord(persistVehicle(), EXISTING_START, EXISTING_END, "100.00", MaintenanceStatus.PLANNED);

        assertThat(maintenanceRepository.findAllByStatusOrderByStartDateAsc(MaintenanceStatus.COMPLETED)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Inclusive overlap query. Existing record: [Jun 10, Jun 20], status PLANNED.
    // ---------------------------------------------------------------------

    @Test
    void overlapDetectsNormalOverlap() {
        Vehicle vehicle = vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.PLANNED);

        assertThat(overlaps(vehicle.getId(), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 25), BLOCKING)).isTrue();
    }

    @Test
    void overlapTreatsEqualBoundaryDatesAsOverlapping() {
        Vehicle vehicle = vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.PLANNED);

        // Candidate starts exactly on the existing end date -> inclusive overlap.
        assertThat(overlaps(vehicle.getId(), EXISTING_END, LocalDate.of(2026, 6, 25), BLOCKING)).isTrue();
        // Candidate ends exactly on the existing start date -> inclusive overlap.
        assertThat(overlaps(vehicle.getId(), LocalDate.of(2026, 6, 1), EXISTING_START, BLOCKING)).isTrue();
    }

    @Test
    void overlapReturnsFalseForPeriodEntirelyBefore() {
        Vehicle vehicle = vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.PLANNED);

        assertThat(overlaps(vehicle.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), BLOCKING)).isFalse();
    }

    @Test
    void overlapReturnsFalseForPeriodEntirelyAfter() {
        Vehicle vehicle = vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.PLANNED);

        assertThat(overlaps(vehicle.getId(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 30), BLOCKING)).isFalse();
    }

    @Test
    void overlapIgnoresRecordWithNonBlockingStatus() {
        Vehicle vehicle = vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.COMPLETED);

        // Same period, but COMPLETED is not in the supplied blocking collection.
        assertThat(overlaps(vehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING)).isFalse();
    }

    @Test
    void overlapIgnoresRecordForAnotherVehicle() {
        vehicleWithRecord(EXISTING_START, EXISTING_END, MaintenanceStatus.PLANNED);
        Vehicle otherVehicle = persistVehicle();

        assertThat(overlaps(otherVehicle.getId(), EXISTING_START, EXISTING_END, BLOCKING)).isFalse();
    }

    // ---------------------------------------------------------------------
    // Foreign-key protection: no cascade delete between MaintenanceRecord and Vehicle
    // ---------------------------------------------------------------------

    @Test
    void deletingRecordDoesNotDeleteVehicle() {
        Vehicle vehicle = persistVehicle();
        MaintenanceRecord record = persistRecord(vehicle, EXISTING_START, EXISTING_END, "100.00", null);

        maintenanceRepository.delete(record);
        maintenanceRepository.flush();
        entityManager.clear();

        assertThat(vehicleRepository.findById(vehicle.getId())).isPresent();
    }

    @Test
    void deletingReferencedVehicleIsBlockedAndDoesNotCascade() {
        Vehicle vehicle = persistVehicle();
        persistRecord(vehicle, EXISTING_START, EXISTING_END, "100.00", null);

        entityManager.flush();
        entityManager.clear();

        // No CascadeType.REMOVE exists, so the maintenance_records.vehicle_id foreign
        // key must block deleting a vehicle still referenced by a record. The thrown
        // exception proves both: the delete is rejected AND the record was not
        // cascade-deleted (a cascade would have removed it and let the delete succeed).
        Long vehicleId = vehicle.getId();
        assertThatThrownBy(() -> {
            vehicleRepository.deleteById(vehicleId);
            vehicleRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}

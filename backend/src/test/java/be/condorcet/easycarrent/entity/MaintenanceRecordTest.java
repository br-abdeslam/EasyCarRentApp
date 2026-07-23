package be.condorcet.easycarrent.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit tests for the {@link MaintenanceRecord} entity contract. No
 * persistence is involved, so no repository is required at this step.
 */
class MaintenanceRecordTest {

    private Vehicle sampleVehicle() {
        return new Vehicle("1-ABC-123", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("45.00"), 12000L, null);
    }

    private MaintenanceRecord sampleRecord() {
        return new MaintenanceRecord(sampleVehicle(), "Full brake system replacement",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), new BigDecimal("1500.00"));
    }

    @Test
    void constructorStoresVehicleDescriptionDatesAndCost() {
        Vehicle vehicle = sampleVehicle();
        MaintenanceRecord record = new MaintenanceRecord(vehicle, "Oil change",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), new BigDecimal("120.00"));

        assertThat(record.getVehicle()).isSameAs(vehicle);
        assertThat(record.getDescription()).isEqualTo("Oil change");
        assertThat(record.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(record.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(record.getCost()).isEqualByComparingTo("120.00");
    }

    @Test
    void newRecordDefaultsToPlanned() {
        assertThat(sampleRecord().getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
    }

    @Test
    void transientRecordsWithNullIdsAreNotEqual() {
        MaintenanceRecord a = sampleRecord();
        MaintenanceRecord b = sampleRecord();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isEqualTo(a);
    }

    @Test
    void recordsWithSameNonNullIdAreEqualAndShareHashCode() {
        MaintenanceRecord a = sampleRecord();
        MaintenanceRecord b = new MaintenanceRecord(sampleVehicle(), "Different work",
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2), new BigDecimal("9.99"));
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 1L);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void recordsWithDifferentIdsAreNotEqual() {
        MaintenanceRecord a = sampleRecord();
        MaintenanceRecord b = sampleRecord();
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentTypeIsNotEqual() {
        assertThat(sampleRecord()).isNotEqualTo("not a maintenance record");
    }

    @Test
    void toStringContainsIdAndStatus() {
        MaintenanceRecord record = sampleRecord();
        ReflectionTestUtils.setField(record, "id", 7L);

        assertThat(record.toString()).contains("7").contains("PLANNED");
    }

    @Test
    void toStringDoesNotExposeVehicle() {
        MaintenanceRecord record = sampleRecord();
        ReflectionTestUtils.setField(record, "id", 7L);

        String text = record.toString();

        assertThat(text).doesNotContain("Vehicle").doesNotContain("1-ABC-123").doesNotContain("Toyota");
    }

    @Test
    void toStringDoesNotExposeCost() {
        MaintenanceRecord record = sampleRecord();
        ReflectionTestUtils.setField(record, "id", 7L);

        assertThat(record.toString()).doesNotContain("1500");
    }

    @Test
    void toStringDoesNotExposeCompleteDescription() {
        MaintenanceRecord record = sampleRecord();
        ReflectionTestUtils.setField(record, "id", 7L);

        assertThat(record.toString()).doesNotContain("brake");
    }

    // ------------------------------------------------------------------ lifecycle

    private MaintenanceRecord recordFor(Vehicle vehicle) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, "Full brake system replacement",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), new BigDecimal("1500.00"));
        ReflectionTestUtils.setField(record, "id", 5L);
        return record;
    }

    private void assertFieldsPreserved(MaintenanceRecord record, Vehicle vehicle) {
        assertThat(record.getId()).isEqualTo(5L);
        assertThat(record.getVehicle()).isSameAs(vehicle);
        assertThat(record.getDescription()).isEqualTo("Full brake system replacement");
        assertThat(record.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(record.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(record.getCost()).isEqualByComparingTo("1500.00");
    }

    @Test
    void startChangesPlannedToInProgress() {
        MaintenanceRecord record = sampleRecord();

        record.start();

        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
    }

    @Test
    void completeChangesInProgressToCompleted() {
        MaintenanceRecord record = sampleRecord();
        record.start();

        record.complete();

        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    void startPreservesEveryOtherField() {
        Vehicle vehicle = sampleVehicle();
        MaintenanceRecord record = recordFor(vehicle);

        record.start();

        assertFieldsPreserved(record, vehicle);
    }

    @Test
    void completePreservesEveryOtherField() {
        Vehicle vehicle = sampleVehicle();
        MaintenanceRecord record = recordFor(vehicle);

        record.start();
        record.complete();

        assertFieldsPreserved(record, vehicle);
    }
}

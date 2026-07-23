package be.condorcet.easycarrent.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceRecord;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleStatus;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit tests for {@link MaintenanceRecordMapper} and the maintenance DTO
 * contracts. No Spring context or persistence is involved.
 */
class MaintenanceRecordMapperTest {

    private final MaintenanceRecordMapper mapper = new MaintenanceRecordMapper();

    private Vehicle vehicleWith(Long id) {
        Vehicle vehicle = new Vehicle("REG-1", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, null);
        ReflectionTestUtils.setField(vehicle, "id", id);
        return vehicle;
    }

    private MaintenanceRecordRequestDto request(Long vehicleId, String cost) {
        return new MaintenanceRecordRequestDto(vehicleId, "Full brake service",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), new BigDecimal(cost));
    }

    private MaintenanceRecord recordWith(Long id, Vehicle vehicle, MaintenanceStatus status) {
        MaintenanceRecord record = new MaintenanceRecord(vehicle, "Full brake service",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), new BigDecimal("120.00"));
        ReflectionTestUtils.setField(record, "id", id);
        if (status != null) {
            ReflectionTestUtils.setField(record, "status", status);
        }
        return record;
    }

    // ------------------------------------------------------------------ toEntity

    @Test
    void toEntityUsesResolvedVehicleDescriptionDatesAndNormalizedCost() {
        Vehicle vehicle = vehicleWith(42L);
        // Request cost differs from the normalized cost the service passes in.
        MaintenanceRecordRequestDto request = request(42L, "999.99");

        MaintenanceRecord entity = mapper.toEntity(request, vehicle, new BigDecimal("120.00"));

        assertThat(entity.getVehicle()).isSameAs(vehicle);
        assertThat(entity.getDescription()).isEqualTo("Full brake service");
        assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 20));
        // The mapper uses the normalized cost, not the raw request cost.
        assertThat(entity.getCost()).isEqualByComparingTo("120.00");
        assertThat(entity.getCost()).isNotEqualByComparingTo("999.99");
    }

    @Test
    void toEntityCreatesPlannedStatus() {
        MaintenanceRecord entity = mapper.toEntity(request(42L, "120.00"), vehicleWith(42L), new BigDecimal("120.00"));

        assertThat(entity.getStatus()).isEqualTo(MaintenanceStatus.PLANNED);
    }

    @Test
    void toEntityDoesNotModifyVehicle() {
        Vehicle vehicle = vehicleWith(42L);

        mapper.toEntity(request(42L, "120.00"), vehicle, new BigDecimal("120.00"));

        assertThat(vehicle.getId()).isEqualTo(42L);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getRegistrationNumber()).isEqualTo("REG-1");
    }

    // ------------------------------------------------------------------ toResponse

    @Test
    void toResponseMapsAllFields() {
        Vehicle vehicle = vehicleWith(42L);
        MaintenanceRecord record = recordWith(7L, vehicle, MaintenanceStatus.IN_PROGRESS);

        MaintenanceRecordResponseDto response = mapper.toResponse(record);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.vehicleId()).isEqualTo(42L);
        assertThat(response.description()).isEqualTo("Full brake service");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(response.cost()).isEqualByComparingTo("120.00");
        assertThat(response.status()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
    }

    // ------------------------------------------------------------------ DTO contracts

    @Test
    void requestDtoContainsExactlyFiveExpectedFields() {
        assertThat(MaintenanceRecordRequestDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("vehicleId", "description", "startDate", "endDate", "cost");
    }

    @Test
    void requestDtoDoesNotExposeStatusIdOrEntities() throws NoSuchFieldException {
        assertThat(MaintenanceRecordRequestDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("id", "status", "vehicle", "createdAt", "updatedAt");
        // vehicleId is a plain Long identifier, never a Vehicle entity.
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("vehicleId").getType()).isEqualTo(Long.class);
    }

    @Test
    void requestDtoFieldsCarryRequiredValidationAnnotations() throws NoSuchFieldException {
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("vehicleId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("startDate").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("endDate").isAnnotationPresent(NotNull.class)).isTrue();

        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("description").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("description").isAnnotationPresent(Size.class)).isTrue();

        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("cost").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("cost").isAnnotationPresent(PositiveOrZero.class)).isTrue();
        assertThat(MaintenanceRecordRequestDto.class.getDeclaredField("cost").isAnnotationPresent(Digits.class)).isTrue();
    }

    @Test
    void responseDtoContainsExactlySevenExpectedFields() {
        assertThat(MaintenanceRecordResponseDto.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("id", "vehicleId", "description", "startDate", "endDate", "cost", "status");
    }

    @Test
    void dtosExposeNoJpaEntityType() {
        for (RecordComponent rc : MaintenanceRecordRequestDto.class.getRecordComponents()) {
            assertThat(rc.getType().isAnnotationPresent(Entity.class))
                    .as("request component %s must not be a JPA entity", rc.getName())
                    .isFalse();
        }
        for (RecordComponent rc : MaintenanceRecordResponseDto.class.getRecordComponents()) {
            assertThat(rc.getType().isAnnotationPresent(Entity.class))
                    .as("response component %s must not be a JPA entity", rc.getName())
                    .isFalse();
        }
    }
}

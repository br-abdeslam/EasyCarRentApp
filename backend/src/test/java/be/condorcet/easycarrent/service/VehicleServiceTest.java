package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.VehicleMapper;
import be.condorcet.easycarrent.repository.RentalRepository;
import be.condorcet.easycarrent.repository.VehicleCategoryRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleCategoryRepository categoryRepository;

    @Mock
    private RentalRepository rentalRepository;

    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(vehicleRepository, categoryRepository, rentalRepository, new VehicleMapper());
    }

    private VehicleCategory categoryWithId(Long id, String name) {
        VehicleCategory category = new VehicleCategory(name, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private VehicleRequestDto request(String registration, Long categoryId) {
        return new VehicleRequestDto(registration, "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryId);
    }

    @Test
    void createPersistsAndDefaultsStatusToAvailable() {
        when(vehicleRepository.existsByRegistrationNumberIgnoreCase("1-ABC-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV")));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponseDto result = service.create(request("1-ABC-001", 1L));

        assertThat(result.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(result.registrationNumber()).isEqualTo("1-ABC-001");
        assertThat(result.categoryId()).isEqualTo(1L);
        assertThat(result.categoryName()).isEqualTo("SUV");
    }

    @Test
    void createRejectsDuplicateRegistrationCaseInsensitively() {
        when(vehicleRepository.existsByRegistrationNumberIgnoreCase("1-abc-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("1-abc-001", 1L)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingCategory() {
        when(vehicleRepository.existsByRegistrationNumberIgnoreCase("1-ABC-001")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("1-ABC-001", 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStatusReturnsMappedVehicles() {
        Vehicle vehicle = new Vehicle("1-MNT-006", "Ford", "Focus", 2021, "Red",
                new BigDecimal("39.50"), 20000L, VehicleStatus.MAINTENANCE, categoryWithId(1L, "SUV"));
        when(vehicleRepository.findAllByStatus(VehicleStatus.MAINTENANCE)).thenReturn(List.of(vehicle));

        List<VehicleResponseDto> result = service.findByStatus(VehicleStatus.MAINTENANCE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void updateAppliesChanges() {
        Vehicle existing = new Vehicle("1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.findByRegistrationNumberIgnoreCase("1-ABC-001")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV")));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponseDto result = service.update(1L,
                new VehicleRequestDto("1-ABC-001", "Toyota", "Yaris", 2023, "Green",
                        new BigDecimal("55.00"), 16000L, 1L));

        assertThat(result.model()).isEqualTo("Yaris");
        assertThat(result.dailyPrice()).isEqualByComparingTo("55.00");
    }

    @Test
    void updateRejectsDuplicateRegistrationOwnedByAnotherVehicle() {
        Vehicle target = new Vehicle("1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(target, "id", 1L);
        Vehicle other = new Vehicle("1-DUP-002", "Ford", "Focus", 2021, "Red",
                new BigDecimal("39.50"), 20000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(other, "id", 2L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(target));
        when(vehicleRepository.findByRegistrationNumberIgnoreCase("1-DUP-002")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L,
                new VehicleRequestDto("1-DUP-002", "Toyota", "Corolla", 2022, "Blue",
                        new BigDecimal("49.99"), 15000L, 1L)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenVehicleMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("1-ABC-001", 1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateThrowsWhenCategoryMissing() {
        Vehicle existing = new Vehicle("1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.findByRegistrationNumberIgnoreCase("1-ABC-001")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request("1-ABC-001", 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesUnreferencedVehicle() {
        Vehicle vehicle = new Vehicle("1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(vehicle, "id", 1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(rentalRepository.existsByVehicle_Id(1L)).thenReturn(false);

        service.delete(1L);

        verify(rentalRepository).existsByVehicle_Id(1L);
        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteReferencedVehicleThrowsConflict() {
        Vehicle vehicle = new Vehicle("1-ABC-001", "Toyota", "Corolla", 2022, "Blue",
                new BigDecimal("49.99"), 15000L, categoryWithId(1L, "SUV"));
        ReflectionTestUtils.setField(vehicle, "id", 1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(rentalRepository.existsByVehicle_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class);
        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

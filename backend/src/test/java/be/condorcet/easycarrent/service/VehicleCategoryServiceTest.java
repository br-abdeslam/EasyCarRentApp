package be.condorcet.easycarrent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.condorcet.easycarrent.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.entity.VehicleCategory;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceConflictException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.VehicleCategoryMapper;
import be.condorcet.easycarrent.repository.VehicleCategoryRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VehicleCategoryServiceTest {

    @Mock
    private VehicleCategoryRepository categoryRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleCategoryService service;

    @BeforeEach
    void setUp() {
        service = new VehicleCategoryService(categoryRepository, vehicleRepository, new VehicleCategoryMapper());
    }

    private VehicleCategory categoryWithId(Long id, String name, String description) {
        VehicleCategory category = new VehicleCategory(name, description);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    void findAllReturnsMappedCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                categoryWithId(1L, "SUV", "Sport"),
                categoryWithId(2L, "Economy", "Budget")));

        List<VehicleCategoryResponseDto> result = service.findAll();

        assertThat(result).extracting(VehicleCategoryResponseDto::name)
                .containsExactly("SUV", "Economy");
    }

    @Test
    void findByIdReturnsCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV", "Sport")));

        VehicleCategoryResponseDto result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("SUV");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPersistsWhenNameIsUnique() {
        VehicleCategoryRequestDto request = new VehicleCategoryRequestDto("SUV", "Sport");
        when(categoryRepository.existsByNameIgnoreCase("SUV")).thenReturn(false);
        when(categoryRepository.save(any(VehicleCategory.class)))
                .thenReturn(categoryWithId(1L, "SUV", "Sport"));

        VehicleCategoryResponseDto result = service.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("SUV");
    }

    @Test
    void createRejectsDuplicateNameCaseInsensitively() {
        when(categoryRepository.existsByNameIgnoreCase("suv")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new VehicleCategoryRequestDto("suv", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateAppliesChanges() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV", "Old")));
        when(categoryRepository.findByNameIgnoreCase("SUV")).thenReturn(Optional.of(categoryWithId(1L, "SUV", "Old")));
        when(categoryRepository.save(any(VehicleCategory.class)))
                .thenReturn(categoryWithId(1L, "SUV", "New"));

        VehicleCategoryResponseDto result = service.update(1L, new VehicleCategoryRequestDto("SUV", "New"));

        assertThat(result.description()).isEqualTo("New");
    }

    @Test
    void updateRejectsDuplicateNameOwnedByAnotherCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV", "Old")));
        when(categoryRepository.findByNameIgnoreCase("Economy"))
                .thenReturn(Optional.of(categoryWithId(2L, "Economy", "Other")));

        assertThatThrownBy(() -> service.update(1L, new VehicleCategoryRequestDto("Economy", "x")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new VehicleCategoryRequestDto("SUV", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesUnusedCategory() {
        VehicleCategory category = categoryWithId(1L, "SUV", "Sport");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(vehicleRepository.existsByCategory_Id(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteRejectsCategoryReferencedByVehicles() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoryWithId(1L, "SUV", "Sport")));
        when(vehicleRepository.existsByCategory_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

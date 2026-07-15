package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.entity.VehicleCategory;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link VehicleCategory} entities and their DTOs.
 */
@Component
public class VehicleCategoryMapper {

    public VehicleCategory toEntity(VehicleCategoryRequestDto request) {
        return new VehicleCategory(request.name(), request.description());
    }

    public void updateEntity(VehicleCategory category, VehicleCategoryRequestDto request) {
        category.setName(request.name());
        category.setDescription(request.description());
    }

    public VehicleCategoryResponseDto toResponse(VehicleCategory category) {
        return new VehicleCategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDescription());
    }
}

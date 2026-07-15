package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Vehicle} entities and their DTOs.
 *
 * <p>The owning {@link VehicleCategory} is resolved by the service layer and
 * passed in; this mapper never queries repositories.
 */
@Component
public class VehicleMapper {

    /**
     * Builds a new vehicle from the request. The status defaults to
     * {@code AVAILABLE} through the entity constructor.
     */
    public Vehicle toEntity(VehicleRequestDto request, VehicleCategory category) {
        return new Vehicle(
                request.registrationNumber(),
                request.brand(),
                request.model(),
                request.manufacturingYear(),
                request.color(),
                request.dailyPrice(),
                request.mileage(),
                category);
    }

    /**
     * Applies the editable request fields onto an existing vehicle. The status
     * is intentionally left unchanged because it is not part of the request
     * contract.
     */
    public void updateEntity(Vehicle vehicle, VehicleRequestDto request, VehicleCategory category) {
        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setManufacturingYear(request.manufacturingYear());
        vehicle.setColor(request.color());
        vehicle.setDailyPrice(request.dailyPrice());
        vehicle.setMileage(request.mileage());
        vehicle.setCategory(category);
    }

    public VehicleResponseDto toResponse(Vehicle vehicle) {
        VehicleCategory category = vehicle.getCategory();
        return new VehicleResponseDto(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufacturingYear(),
                vehicle.getColor(),
                vehicle.getDailyPrice(),
                vehicle.getMileage(),
                vehicle.getStatus(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null);
    }
}

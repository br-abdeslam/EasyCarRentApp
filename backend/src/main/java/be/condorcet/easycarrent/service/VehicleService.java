package be.condorcet.easycarrent.service;

import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.Vehicle;
import be.condorcet.easycarrent.entity.VehicleCategory;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.exception.DuplicateResourceException;
import be.condorcet.easycarrent.exception.ResourceNotFoundException;
import be.condorcet.easycarrent.mapper.VehicleMapper;
import be.condorcet.easycarrent.repository.VehicleCategoryRepository;
import be.condorcet.easycarrent.repository.VehicleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing vehicles.
 */
@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final VehicleMapper mapper;

    public VehicleService(VehicleRepository vehicleRepository,
                          VehicleCategoryRepository categoryRepository,
                          VehicleMapper mapper) {
        this.vehicleRepository = vehicleRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    public List<VehicleResponseDto> findAll() {
        return vehicleRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public VehicleResponseDto findById(Long id) {
        return mapper.toResponse(getVehicle(id));
    }

    public List<VehicleResponseDto> findByStatus(VehicleStatus status) {
        return vehicleRepository.findAllByStatus(status).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public VehicleResponseDto create(VehicleRequestDto request) {
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
            throw new DuplicateResourceException(
                    "A vehicle with registration number '" + request.registrationNumber() + "' already exists");
        }
        VehicleCategory category = getCategory(request.categoryId());
        Vehicle saved = vehicleRepository.save(mapper.toEntity(request, category));
        return mapper.toResponse(saved);
    }

    @Transactional
    public VehicleResponseDto update(Long id, VehicleRequestDto request) {
        Vehicle vehicle = getVehicle(id);
        vehicleRepository.findByRegistrationNumberIgnoreCase(request.registrationNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A vehicle with registration number '" + request.registrationNumber()
                                    + "' already exists");
                });
        VehicleCategory category = getCategory(request.categoryId());
        mapper.updateEntity(vehicle, request, category);
        return mapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = getVehicle(id);
        vehicleRepository.delete(vehicle);
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id " + id));
    }

    private VehicleCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }
}

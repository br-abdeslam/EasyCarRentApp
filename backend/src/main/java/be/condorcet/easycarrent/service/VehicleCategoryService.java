package be.condorcet.easycarrent.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing vehicle categories.
 */
@Service
@Transactional(readOnly = true)
public class VehicleCategoryService {

    private final VehicleCategoryRepository categoryRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryMapper mapper;

    public VehicleCategoryService(VehicleCategoryRepository categoryRepository,
                                  VehicleRepository vehicleRepository,
                                  VehicleCategoryMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.vehicleRepository = vehicleRepository;
        this.mapper = mapper;
    }

    public List<VehicleCategoryResponseDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public VehicleCategoryResponseDto findById(Long id) {
        return mapper.toResponse(getCategory(id));
    }

    @Transactional
    public VehicleCategoryResponseDto create(VehicleCategoryRequestDto request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists");
        }
        VehicleCategory saved = categoryRepository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional
    public VehicleCategoryResponseDto update(Long id, VehicleCategoryRequestDto request) {
        VehicleCategory category = getCategory(id);
        categoryRepository.findByNameIgnoreCase(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A category named '" + request.name() + "' already exists");
                });
        mapper.updateEntity(category, request);
        return mapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        VehicleCategory category = getCategory(id);
        if (vehicleRepository.existsByCategory_Id(id)) {
            throw new ResourceConflictException(
                    "Category '" + category.getName() + "' cannot be deleted while vehicles reference it");
        }
        categoryRepository.delete(category);
    }

    private VehicleCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }
}

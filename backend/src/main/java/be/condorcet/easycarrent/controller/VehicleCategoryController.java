package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.service.VehicleCategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST endpoints for vehicle categories. HTTP concerns only; all business
 * logic lives in {@link VehicleCategoryService}.
 */
@RestController
@RequestMapping("/api/categories")
public class VehicleCategoryController {

    private final VehicleCategoryService categoryService;

    public VehicleCategoryController(VehicleCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<VehicleCategoryResponseDto> list() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public VehicleCategoryResponseDto get(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @PostMapping
    public ResponseEntity<VehicleCategoryResponseDto> create(
            @Valid @RequestBody VehicleCategoryRequestDto request) {
        VehicleCategoryResponseDto created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public VehicleCategoryResponseDto update(@PathVariable Long id,
                                             @Valid @RequestBody VehicleCategoryRequestDto request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

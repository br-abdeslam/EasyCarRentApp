package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.service.VehicleService;
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
 * REST endpoints for vehicles. HTTP concerns only; all business logic lives in
 * {@link VehicleService}.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponseDto> list() {
        return vehicleService.findAll();
    }

    @GetMapping("/{id}")
    public VehicleResponseDto get(@PathVariable Long id) {
        return vehicleService.findById(id);
    }

    @GetMapping("/status/{status}")
    public List<VehicleResponseDto> listByStatus(@PathVariable VehicleStatus status) {
        return vehicleService.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDto> create(@Valid @RequestBody VehicleRequestDto request) {
        VehicleResponseDto created = vehicleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public VehicleResponseDto update(@PathVariable Long id,
                                     @Valid @RequestBody VehicleRequestDto request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

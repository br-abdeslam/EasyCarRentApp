package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.VehicleRequestDto;
import be.condorcet.easycarrent.dto.VehicleResponseDto;
import be.condorcet.easycarrent.entity.VehicleStatus;
import be.condorcet.easycarrent.service.RentalService;
import be.condorcet.easycarrent.service.VehicleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final RentalService rentalService;

    public VehicleController(VehicleService vehicleService, RentalService rentalService) {
        this.vehicleService = vehicleService;
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<VehicleResponseDto> list() {
        return vehicleService.findAll();
    }

    /**
     * Vehicles bookable for the given inclusive date range. Declared before the
     * {@code /{id}} mapping conceptually; the literal {@code /available} segment
     * always wins over the numeric path variable in Spring's matching.
     */
    @GetMapping("/available")
    public List<VehicleResponseDto> listAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rentalService.findAvailableVehicles(startDate, endDate);
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

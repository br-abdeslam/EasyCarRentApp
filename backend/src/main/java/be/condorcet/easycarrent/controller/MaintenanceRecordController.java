package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.MaintenanceRecordRequestDto;
import be.condorcet.easycarrent.dto.MaintenanceRecordResponseDto;
import be.condorcet.easycarrent.entity.MaintenanceStatus;
import be.condorcet.easycarrent.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST endpoints for maintenance records. HTTP concerns only; all query,
 * creation, overlap, lifecycle, vehicle-synchronization and deletion rules live
 * in {@link MaintenanceRecordService}.
 */
@RestController
@RequestMapping("/api/maintenance-records")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping
    public List<MaintenanceRecordResponseDto> list() {
        return maintenanceRecordService.findAll();
    }

    @GetMapping("/{id}")
    public MaintenanceRecordResponseDto get(@PathVariable Long id) {
        return maintenanceRecordService.findById(id);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<MaintenanceRecordResponseDto> getByVehicle(@PathVariable Long vehicleId) {
        return maintenanceRecordService.findByVehicleId(vehicleId);
    }

    @GetMapping("/status/{status}")
    public List<MaintenanceRecordResponseDto> getByStatus(@PathVariable MaintenanceStatus status) {
        return maintenanceRecordService.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecordResponseDto> create(
            @Valid @RequestBody MaintenanceRecordRequestDto request) {
        MaintenanceRecordResponseDto created = maintenanceRecordService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/start")
    public MaintenanceRecordResponseDto start(@PathVariable Long id) {
        return maintenanceRecordService.start(id);
    }

    @PatchMapping("/{id}/complete")
    public MaintenanceRecordResponseDto complete(@PathVariable Long id) {
        return maintenanceRecordService.complete(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        maintenanceRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

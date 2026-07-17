package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.RentalRequestDto;
import be.condorcet.easycarrent.dto.RentalResponseDto;
import be.condorcet.easycarrent.service.RentalService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST endpoints for rentals. HTTP concerns only; all booking rules, price
 * calculation and lifecycle logic live in {@link RentalService}.
 */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<RentalResponseDto> list() {
        return rentalService.findAll();
    }

    @GetMapping("/{id}")
    public RentalResponseDto get(@PathVariable Long id) {
        return rentalService.findById(id);
    }

    @PostMapping
    public ResponseEntity<RentalResponseDto> create(@Valid @RequestBody RentalRequestDto request) {
        RentalResponseDto created = rentalService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public RentalResponseDto update(@PathVariable Long id,
                                    @Valid @RequestBody RentalRequestDto request) {
        return rentalService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rentalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start")
    public RentalResponseDto start(@PathVariable Long id) {
        return rentalService.start(id);
    }

    @PatchMapping("/{id}/complete")
    public RentalResponseDto complete(@PathVariable Long id) {
        return rentalService.complete(id);
    }

    @PatchMapping("/{id}/cancel")
    public RentalResponseDto cancel(@PathVariable Long id) {
        return rentalService.cancel(id);
    }
}

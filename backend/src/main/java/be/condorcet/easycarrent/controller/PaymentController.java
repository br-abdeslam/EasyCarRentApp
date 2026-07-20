package be.condorcet.easycarrent.controller;

import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.service.PaymentService;
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
 * REST endpoints for payments. HTTP concerns only; all creation rules, price
 * derivation and lifecycle logic live in {@link PaymentService}.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponseDto> list() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    public PaymentResponseDto get(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    @GetMapping("/rental/{rentalId}")
    public PaymentResponseDto getByRental(@PathVariable Long rentalId) {
        return paymentService.findByRentalId(rentalId);
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> create(@Valid @RequestBody PaymentRequestDto request) {
        PaymentResponseDto created = paymentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/pay")
    public PaymentResponseDto pay(@PathVariable Long id) {
        return paymentService.markPaid(id);
    }

    @PatchMapping("/{id}/fail")
    public PaymentResponseDto fail(@PathVariable Long id) {
        return paymentService.markFailed(id);
    }

    @PatchMapping("/{id}/retry")
    public PaymentResponseDto retry(@PathVariable Long id) {
        return paymentService.retry(id);
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponseDto refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

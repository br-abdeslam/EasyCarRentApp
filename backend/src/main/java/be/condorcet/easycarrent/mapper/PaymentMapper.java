package be.condorcet.easycarrent.mapper;

import be.condorcet.easycarrent.dto.PaymentRequestDto;
import be.condorcet.easycarrent.dto.PaymentResponseDto;
import be.condorcet.easycarrent.entity.Payment;
import be.condorcet.easycarrent.entity.Rental;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Payment} entities and their DTOs.
 *
 * <p>The referenced {@link Rental} and the backend-calculated amount are
 * resolved by the service layer and passed in; this mapper never queries
 * repositories, calculates amounts or makes business decisions.
 */
@Component
public class PaymentMapper {

    /**
     * Builds a new payment from the request, the resolved rental and the
     * backend-calculated amount. The status defaults to {@code PENDING} through the
     * entity constructor and {@code paidAt} stays null. The amount comes from the
     * backend, never from client input (the request carries no amount).
     */
    public Payment toEntity(PaymentRequestDto request, Rental rental, BigDecimal backendAmount) {
        return new Payment(rental, request.paymentMethod(), backendAmount);
    }

    public PaymentResponseDto toResponse(Payment payment) {
        Rental rental = payment.getRental();
        return new PaymentResponseDto(
                payment.getId(),
                rental != null ? rental.getId() : null,
                rental != null ? rental.getStatus() : null,
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt());
    }
}

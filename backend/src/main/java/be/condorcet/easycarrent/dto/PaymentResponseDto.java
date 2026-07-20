package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.entity.PaymentMethod;
import be.condorcet.easycarrent.entity.PaymentStatus;
import be.condorcet.easycarrent.entity.RentalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload exposing a payment to API consumers.
 *
 * <p>Only the identifying rental reference and the payment details are exposed.
 * No entity, customer, vehicle or financial-sensitive data is included.
 */
public record PaymentResponseDto(
        Long id,
        Long rentalId,
        RentalStatus rentalStatus,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}

package be.condorcet.easycarrent.desktop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Client-side representation of a payment returned by the backend.
 *
 * <p>Mirrors the backend response contract exactly. The rental is referenced only
 * by its id and current status (no nested rental, customer, or vehicle data). The
 * backend-calculated amount is a {@link BigDecimal}; the creation and paid
 * timestamps are {@link LocalDateTime} ({@code paidAt} is null until the payment is
 * paid). This response is never used as a request body.</p>
 *
 * @param id            the payment identifier
 * @param rentalId      the settled rental's id
 * @param rentalStatus  the settled rental's current status
 * @param amount        the backend-calculated amount (from the rental total)
 * @param paymentMethod the chosen payment method
 * @param status        the backend-managed payment status
 * @param createdAt     the creation timestamp
 * @param paidAt        the paid timestamp, or null when not paid
 */
public record PaymentResponseDto(
		Long id,
		Long rentalId,
		RentalStatus rentalStatus,
		BigDecimal amount,
		PaymentMethod paymentMethod,
		PaymentStatus status,
		LocalDateTime createdAt,
		LocalDateTime paidAt) {
}

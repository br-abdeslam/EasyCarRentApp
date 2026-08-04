package be.condorcet.easycarrent.desktop.dto;

/**
 * Request payload used to create a payment for a rental.
 *
 * <p>Mirrors the backend request contract exactly: the client supplies only the
 * rental identifier and the chosen method. The amount is computed by the backend
 * from the rental total and the status is backend-controlled, so neither — nor the
 * identifier or timestamps — is ever serialized in a request.</p>
 *
 * @param rentalId      the identifier of the rental to pay for
 * @param paymentMethod the chosen payment method
 */
public record PaymentRequestDto(
		Long rentalId,
		PaymentMethod paymentMethod) {
}

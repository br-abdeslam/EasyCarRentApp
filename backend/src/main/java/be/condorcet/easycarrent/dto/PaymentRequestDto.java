package be.condorcet.easycarrent.dto;

import be.condorcet.easycarrent.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload used to create a payment for a rental.
 *
 * <p>The client supplies only the rental and the chosen method. The amount is
 * computed by the backend from the rental total and the status is
 * backend-controlled, so neither is part of this contract.
 */
public record PaymentRequestDto(

        @NotNull(message = "rentalId is required")
        Long rentalId,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod
) {
}

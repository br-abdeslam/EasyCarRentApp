package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentRequestDto;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-free local validation for the payment editor.
 *
 * <p>Mirrors only the backend rules that can be checked on the client: the rental
 * and the payment method are both required (the backend {@code @NotNull}
 * constraints). The amount is <em>not</em> validated because the backend derives it
 * from the rental total and the request carries no amount; the status is
 * backend-managed and not part of the request; there is no payment-date field.</p>
 *
 * <p>This validator never decides rental eligibility, duplicate-payment, or
 * amount rules: the payable-status (ACTIVE/COMPLETED) and one-payment-per-rental
 * rules stay authoritative on the backend and surface as conflict responses.
 * Validation runs in a single pass so every local error is reported together, in a
 * deterministic order, without duplicates, and without echoing any entered value. A
 * valid result carries the exact {@link PaymentRequestDto} to submit.</p>
 */
public final class PaymentValidator {

	private PaymentValidator() {
	}

	/** Outcome of a validation pass: the ordered errors and, when valid, the request. */
	public static final class Result {

		private final List<String> errors;
		private final PaymentRequestDto request;

		private Result(List<String> errors, PaymentRequestDto request) {
			this.errors = List.copyOf(errors);
			this.request = request;
		}

		public boolean isValid() {
			return errors.isEmpty();
		}

		public List<String> errors() {
			return errors;
		}

		/**
		 * @return the request to submit
		 * @throws IllegalStateException if the result is not valid
		 */
		public PaymentRequestDto request() {
			if (request == null) {
				throw new IllegalStateException("No request is available for an invalid result");
			}
			return request;
		}
	}

	/**
	 * Validates the editor's selections.
	 *
	 * @param rentalId      the selected rental's id, or null when none is selected
	 * @param paymentMethod the selected method, or null when none is selected
	 * @return the validation result
	 */
	public static Result validate(Long rentalId, PaymentMethod paymentMethod) {
		List<String> errors = new ArrayList<>();

		if (rentalId == null) {
			errors.add("Rental is required.");
		}
		if (paymentMethod == null) {
			errors.add("Payment method is required.");
		}

		if (!errors.isEmpty()) {
			return new Result(errors, null);
		}
		return new Result(errors, new PaymentRequestDto(rentalId, paymentMethod));
	}
}

package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.RentalRequestDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-free local validation for the rental editor.
 *
 * <p>Mirrors only the backend rules that can be checked on the client without a
 * database: the customer, the vehicle, the start date, and the end date are all
 * required (the backend {@code @NotNull} constraints), and the end date must be
 * strictly after the start date (the backend service rule). Past dates are
 * <em>not</em> rejected because the backend accepts them, and equal start/end
 * dates are rejected because the backend requires the end to be strictly after the
 * start.</p>
 *
 * <p>This validator never claims vehicle availability: overlap, maintenance,
 * licence-expiry, and bookable-status rules stay authoritative on the backend and
 * surface as conflict responses. Validation runs in a single pass so every local
 * error is reported together, in a deterministic order, without duplicates, and
 * without echoing any entered value. A valid result carries the exact
 * {@link RentalRequestDto} to submit.</p>
 */
public final class RentalValidator {

	private RentalValidator() {
	}

	/** Outcome of a validation pass: the ordered errors and, when valid, the request. */
	public static final class Result {

		private final List<String> errors;
		private final RentalRequestDto request;

		private Result(List<String> errors, RentalRequestDto request) {
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
		public RentalRequestDto request() {
			if (request == null) {
				throw new IllegalStateException("No request is available for an invalid result");
			}
			return request;
		}
	}

	/**
	 * Validates the editor's selections and dates.
	 *
	 * @param customerId the selected customer's id, or null when none is selected
	 * @param vehicleId  the selected vehicle's id, or null when none is selected
	 * @param startDate  the chosen start date, or null when none is chosen
	 * @param endDate    the chosen end date, or null when none is chosen
	 * @return the validation result
	 */
	public static Result validate(Long customerId, Long vehicleId,
			LocalDate startDate, LocalDate endDate) {
		List<String> errors = new ArrayList<>();

		if (customerId == null) {
			errors.add("Customer is required.");
		}
		if (vehicleId == null) {
			errors.add("Vehicle is required.");
		}
		if (startDate == null) {
			errors.add("Start date is required.");
		}
		if (endDate == null) {
			errors.add("End date is required.");
		}
		if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
			errors.add("End date must be after the start date.");
		}

		if (!errors.isEmpty()) {
			return new Result(errors, null);
		}
		return new Result(errors, new RentalRequestDto(startDate, endDate, vehicleId, customerId));
	}
}

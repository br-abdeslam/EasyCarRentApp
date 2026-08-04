package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.MaintenanceRequestDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX-free local validation for the maintenance editor.
 *
 * <p>Mirrors only the backend rules that can be checked on the client: the vehicle,
 * a non-blank description of at most 500 characters, the start and end dates, and a
 * cost that is zero or positive with at most ten integer digits and two decimals are
 * all required, and the end date must be on or after the start date. Past dates are
 * <em>not</em> rejected because the backend accepts them, and same-day maintenance is
 * allowed. The status is backend-managed and not part of the request.</p>
 *
 * <p>This validator never decides vehicle eligibility, rental/maintenance overlap,
 * or vehicle-status rules: those stay authoritative on the backend and surface as
 * conflict responses. Validation runs in a single pass so every applicable local
 * error is reported together, in a deterministic order, without duplicates, and
 * without echoing any entered value. A valid result carries the exact
 * {@link MaintenanceRequestDto} to submit.</p>
 */
public final class MaintenanceValidator {

	private static final int DESCRIPTION_MAX_LENGTH = 500;
	private static final int COST_MAX_INTEGER_DIGITS = 10;
	private static final int COST_MAX_FRACTION_DIGITS = 2;

	private MaintenanceValidator() {
	}

	/** Outcome of a validation pass: the ordered errors and, when valid, the request. */
	public static final class Result {

		private final List<String> errors;
		private final MaintenanceRequestDto request;

		private Result(List<String> errors, MaintenanceRequestDto request) {
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
		public MaintenanceRequestDto request() {
			if (request == null) {
				throw new IllegalStateException("No request is available for an invalid result");
			}
			return request;
		}
	}

	/**
	 * Validates the editor's selections and inputs.
	 *
	 * @param vehicleId   the selected vehicle's id, or null when none is selected
	 * @param description the entered description, which may be null or blank
	 * @param startDate   the chosen start date, or null when none is chosen
	 * @param endDate     the chosen end date, or null when none is chosen
	 * @param costText    the entered cost text, which may be null or blank
	 * @return the validation result
	 */
	public static Result validate(Long vehicleId, String description,
			LocalDate startDate, LocalDate endDate, String costText) {
		List<String> errors = new ArrayList<>();

		if (vehicleId == null) {
			errors.add("Vehicle is required.");
		}

		String trimmedDescription = description == null ? "" : description.trim();
		if (trimmedDescription.isEmpty()) {
			errors.add("Description is required.");
		} else if (trimmedDescription.length() > DESCRIPTION_MAX_LENGTH) {
			errors.add("Description must not exceed " + DESCRIPTION_MAX_LENGTH + " characters.");
		}

		if (startDate == null) {
			errors.add("Start date is required.");
		}
		if (endDate == null) {
			errors.add("End date is required.");
		}
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			errors.add("End date must be on or after the start date.");
		}

		BigDecimal cost = validateCost(costText, errors);

		if (!errors.isEmpty()) {
			return new Result(errors, null);
		}
		return new Result(errors,
				new MaintenanceRequestDto(vehicleId, trimmedDescription, startDate, endDate, cost));
	}

	/**
	 * Validates the cost text and returns the parsed value, or null when it is
	 * missing or invalid (a single, most-relevant cost error is added).
	 */
	private static BigDecimal validateCost(String costText, List<String> errors) {
		String trimmed = costText == null ? "" : costText.trim();
		if (trimmed.isEmpty()) {
			errors.add("Cost is required.");
			return null;
		}
		BigDecimal cost;
		try {
			cost = new BigDecimal(trimmed);
		} catch (NumberFormatException notANumber) {
			errors.add("Cost must be a valid amount (for example 125.00).");
			return null;
		}
		if (cost.signum() < 0) {
			errors.add("Cost must be zero or positive.");
			return null;
		}
		int fractionDigits = Math.max(cost.scale(), 0);
		if (fractionDigits > COST_MAX_FRACTION_DIGITS) {
			errors.add("Cost must have at most " + COST_MAX_FRACTION_DIGITS + " decimals.");
			return null;
		}
		int integerDigits = cost.precision() - cost.scale();
		if (integerDigits > COST_MAX_INTEGER_DIGITS) {
			errors.add("Cost must not exceed " + COST_MAX_INTEGER_DIGITS
					+ " digits before the decimal point.");
			return null;
		}
		return cost;
	}
}

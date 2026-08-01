package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Client-side validation of customer input, mirroring the backend constraints
 * exactly so obviously invalid requests are not sent.
 *
 * <p>Backend rules (from the customer request contract): first and last name
 * required and at most 60 characters; email required, a valid address, and at
 * most 120 characters; phone required and matching the backend phone pattern;
 * address required and at most 255 characters; driving-licence number required
 * and at most 40 characters; driving-licence expiry required and not in the past.
 * The reference "today" is supplied by the caller so the expiry rule stays
 * deterministic in tests. The backend remains authoritative, and its
 * {@code validationErrors} are still displayed when a request is rejected.</p>
 */
public final class CustomerValidator {

	public static final int NAME_MAX_LENGTH = 60;
	public static final int EMAIL_MAX_LENGTH = 120;
	public static final int ADDRESS_MAX_LENGTH = 255;
	public static final int LICENSE_MAX_LENGTH = 40;

	// A tolerant email check aligned with the lenient backend @Email: a non-empty
	// local part, a single @, and a non-empty domain, with no whitespace.
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+$");

	// Mirrors the backend phone @Pattern exactly.
	private static final Pattern PHONE_PATTERN =
			Pattern.compile("^(?=.*[0-9])\\+?[0-9 ()\\-]{6,20}$");

	private CustomerValidator() {
	}

	/** The outcome of validation: the errors, and the built request when valid. */
	public record Result(List<String> errors, CustomerRequestDto request) {
		public boolean isValid() {
			return errors.isEmpty();
		}
	}

	/**
	 * Validates raw form input and, when valid, builds the exact request DTO.
	 *
	 * @param firstName   the first-name input
	 * @param lastName    the last-name input
	 * @param email       the email input
	 * @param phone       the phone input
	 * @param address     the address input
	 * @param license     the driving-licence number input
	 * @param expiryDate  the selected driving-licence expiry date, or null
	 * @param today       the reference current date for the not-in-the-past rule
	 * @return the validation result
	 */
	public static Result validate(String firstName, String lastName, String email, String phone,
			String address, String license, LocalDate expiryDate, LocalDate today) {
		List<String> errors = new ArrayList<>();

		String firstNameValue = requiredText(firstName, "First name", NAME_MAX_LENGTH, errors);
		String lastNameValue = requiredText(lastName, "Last name", NAME_MAX_LENGTH, errors);

		String emailValue = trimOrEmpty(email);
		if (emailValue.isEmpty()) {
			errors.add("Email is required.");
		} else if (emailValue.length() > EMAIL_MAX_LENGTH) {
			errors.add("Email must be at most " + EMAIL_MAX_LENGTH + " characters.");
		} else if (!EMAIL_PATTERN.matcher(emailValue).matches()) {
			errors.add("Email must be a valid email address.");
		}

		String phoneValue = trimOrEmpty(phone);
		if (phoneValue.isEmpty()) {
			errors.add("Phone is required.");
		} else if (!PHONE_PATTERN.matcher(phoneValue).matches()) {
			errors.add("Phone must contain digits and may include spaces, parentheses, "
					+ "hyphens and an optional leading +.");
		}

		String addressValue = requiredText(address, "Address", ADDRESS_MAX_LENGTH, errors);
		String licenseValue = requiredText(license, "Driving licence number", LICENSE_MAX_LENGTH, errors);

		if (expiryDate == null) {
			errors.add("Driving licence expiry date is required.");
		} else if (expiryDate.isBefore(today)) {
			errors.add("Driving licence expiry date must not be in the past.");
		}

		CustomerRequestDto request = errors.isEmpty()
				? new CustomerRequestDto(firstNameValue, lastNameValue, emailValue, phoneValue,
						addressValue, licenseValue, expiryDate)
				: null;
		return new Result(errors, request);
	}

	private static String requiredText(String value, String label, int maxLength, List<String> errors) {
		String trimmed = trimOrEmpty(value);
		if (trimmed.isEmpty()) {
			errors.add(label + " is required.");
		} else if (trimmed.length() > maxLength) {
			errors.add(label + " must be at most " + maxLength + " characters.");
		}
		return trimmed;
	}

	private static String trimOrEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}

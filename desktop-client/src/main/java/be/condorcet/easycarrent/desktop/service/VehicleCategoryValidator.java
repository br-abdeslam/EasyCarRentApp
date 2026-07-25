package be.condorcet.easycarrent.desktop.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side validation of vehicle-category input, mirroring the backend
 * constraints exactly so obviously invalid requests are not sent.
 *
 * <p>Backend rules: {@code name} is required and at most 100 characters;
 * {@code description} is optional and at most 500 characters. The backend
 * remains authoritative and its {@code validationErrors} are still displayed when
 * a request is rejected server-side.</p>
 */
public final class VehicleCategoryValidator {

	public static final int NAME_MAX_LENGTH = 100;
	public static final int DESCRIPTION_MAX_LENGTH = 500;

	private VehicleCategoryValidator() {
	}

	/**
	 * Validates the given field values.
	 *
	 * @param name        the raw name input (may be null)
	 * @param description the raw description input (may be null)
	 * @return a list of human-readable error messages; empty when the input is valid
	 */
	public static List<String> validate(String name, String description) {
		List<String> errors = new ArrayList<>();
		String trimmedName = name == null ? "" : name.trim();
		if (trimmedName.isEmpty()) {
			errors.add("Name is required.");
		} else if (trimmedName.length() > NAME_MAX_LENGTH) {
			errors.add("Name must be at most " + NAME_MAX_LENGTH + " characters.");
		}
		if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
			errors.add("Description must be at most " + DESCRIPTION_MAX_LENGTH + " characters.");
		}
		return errors;
	}

	/** @return true if the input satisfies the backend constraints */
	public static boolean isValid(String name, String description) {
		return validate(name, description).isEmpty();
	}
}

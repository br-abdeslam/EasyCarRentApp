package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.VehicleRequestDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side validation of vehicle input, mirroring the backend constraints
 * exactly so obviously invalid requests are not sent.
 *
 * <p>Backend rules (from the vehicle request contract and validators):
 * {@code registrationNumber} required and at most 20 chars; {@code brand} and
 * {@code model} required and at most 60 chars; {@code color} optional and at most
 * 40 chars; {@code manufacturingYear} optional but, when present, between
 * {@value #MIN_YEAR} and the current year; {@code dailyPrice} required, positive,
 * and within precision {@value #PRICE_PRECISION} / scale {@value #PRICE_SCALE};
 * {@code mileage} optional and non-negative; {@code categoryId} required. The
 * current year is supplied by the caller so year validation stays deterministic in
 * tests. The backend remains authoritative.</p>
 */
public final class VehicleValidator {

	public static final int REGISTRATION_MAX_LENGTH = 20;
	public static final int BRAND_MAX_LENGTH = 60;
	public static final int MODEL_MAX_LENGTH = 60;
	public static final int COLOR_MAX_LENGTH = 40;
	public static final int MIN_YEAR = 1900;
	public static final int PRICE_PRECISION = 10;
	public static final int PRICE_SCALE = 2;

	private VehicleValidator() {
	}

	/** The outcome of validation: the errors, and the built request when valid. */
	public record Result(List<String> errors, VehicleRequestDto request) {
		public boolean isValid() {
			return errors.isEmpty();
		}
	}

	/**
	 * Validates raw form input and, when valid, builds the exact request DTO.
	 *
	 * @param registration the registration number input
	 * @param brand        the brand input
	 * @param model        the model input
	 * @param yearText     the manufacturing-year input (optional)
	 * @param color        the color input (optional)
	 * @param priceText    the daily-price input
	 * @param mileageText  the mileage input (optional)
	 * @param categoryId   the selected category id, or null if none is selected
	 * @param currentYear  the maximum acceptable manufacturing year
	 * @return the validation result
	 */
	public static Result validate(String registration, String brand, String model, String yearText,
			String color, String priceText, String mileageText, Long categoryId, int currentYear) {
		List<String> errors = new ArrayList<>();

		String reg = trimOrEmpty(registration);
		if (reg.isEmpty()) {
			errors.add("Registration number is required.");
		} else if (reg.length() > REGISTRATION_MAX_LENGTH) {
			errors.add("Registration number must be at most " + REGISTRATION_MAX_LENGTH + " characters.");
		}

		String brandValue = trimOrEmpty(brand);
		if (brandValue.isEmpty()) {
			errors.add("Brand is required.");
		} else if (brandValue.length() > BRAND_MAX_LENGTH) {
			errors.add("Brand must be at most " + BRAND_MAX_LENGTH + " characters.");
		}

		String modelValue = trimOrEmpty(model);
		if (modelValue.isEmpty()) {
			errors.add("Model is required.");
		} else if (modelValue.length() > MODEL_MAX_LENGTH) {
			errors.add("Model must be at most " + MODEL_MAX_LENGTH + " characters.");
		}

		Integer year = null;
		String yearValue = trimOrEmpty(yearText);
		if (!yearValue.isEmpty()) {
			try {
				year = Integer.valueOf(yearValue);
				if (year < MIN_YEAR || year > currentYear) {
					errors.add("Manufacturing year must be between " + MIN_YEAR + " and " + currentYear + ".");
				}
			} catch (NumberFormatException e) {
				errors.add("Manufacturing year must be a whole number.");
				year = null;
			}
		}

		String colorValue = trimOrEmpty(color);
		if (colorValue.length() > COLOR_MAX_LENGTH) {
			errors.add("Color must be at most " + COLOR_MAX_LENGTH + " characters.");
		}

		BigDecimal price = null;
		String priceValue = trimOrEmpty(priceText);
		if (priceValue.isEmpty()) {
			errors.add("Daily price is required.");
		} else {
			try {
				price = new BigDecimal(priceValue);
				if (price.signum() <= 0) {
					errors.add("Daily price must be positive.");
				} else if (price.scale() > PRICE_SCALE) {
					errors.add("Daily price must have at most " + PRICE_SCALE + " decimal places.");
				} else if (price.precision() - price.scale() > PRICE_PRECISION - PRICE_SCALE) {
					errors.add("Daily price is too large.");
				}
			} catch (NumberFormatException e) {
				errors.add("Daily price must be a valid number.");
				price = null;
			}
		}

		Long mileage = null;
		String mileageValue = trimOrEmpty(mileageText);
		if (!mileageValue.isEmpty()) {
			try {
				mileage = Long.valueOf(mileageValue);
				if (mileage < 0) {
					errors.add("Mileage cannot be negative.");
				}
			} catch (NumberFormatException e) {
				errors.add("Mileage must be a whole number.");
				mileage = null;
			}
		}

		if (categoryId == null) {
			errors.add("A category is required.");
		}

		VehicleRequestDto request = errors.isEmpty()
				? new VehicleRequestDto(reg, brandValue, modelValue, year,
						colorValue.isEmpty() ? null : colorValue, price, mileage, categoryId)
				: null;
		return new Result(errors, request);
	}

	private static String trimOrEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}

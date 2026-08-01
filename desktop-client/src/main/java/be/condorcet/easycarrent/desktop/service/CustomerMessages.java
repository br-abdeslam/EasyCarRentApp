package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * JavaFX-free formatting of safe, user-facing messages for the Customers screen.
 *
 * <p>Turns local validation errors and backend failures into short, readable text
 * with one issue per line, deterministic ordering, and no duplicates. It never
 * exposes raw JSON, exception-class names, stack traces, SQL, {@code Authorization}
 * values, passwords, or submitted personal values. Keeping this logic out of the
 * controller makes it independently testable.</p>
 */
public final class CustomerMessages {

	public static final String CONNECTION_UNAVAILABLE =
			"The backend is unavailable. Please try again.";
	public static final String DELETE_CONFLICT =
			"This customer cannot be deleted because one or more rentals reference it.";
	public static final String UNEXPECTED = "An unexpected error occurred.";
	public static final String REQUEST_INVALID = "The request was invalid.";
	public static final String NOT_AUTHORIZED = "You are not authorized to perform this action.";
	public static final String NOT_FOUND = "The customer no longer exists. Please refresh.";
	public static final String CONFLICT_GENERIC = "The request conflicts with existing data.";

	/** Canonical field order so aggregated backend errors are deterministic. */
	private static final List<String> FIELD_ORDER = List.of(
			"firstName", "lastName", "email", "phone", "address",
			"drivingLicenseNumber", "drivingLicenseExpiryDate");

	private static final Map<String, String> FIELD_LABELS = Map.of(
			"firstName", "First name",
			"lastName", "Last name",
			"email", "Email",
			"phone", "Phone",
			"address", "Address",
			"drivingLicenseNumber", "Driving licence number",
			"drivingLicenseExpiryDate", "Driving licence expiry date");

	private CustomerMessages() {
	}

	/**
	 * Joins local validation errors, one per line, preserving the validator's order
	 * and removing exact duplicates.
	 */
	public static String localValidation(List<String> errors) {
		return String.join("\n", localValidationLines(errors));
	}

	/**
	 * Local validation errors as a list of individual display lines, preserving the
	 * validator's order and removing exact duplicates. Each entry is rendered as its
	 * own visible label so every error stays fully readable.
	 */
	public static List<String> localValidationLines(List<String> errors) {
		return errors.stream().distinct().toList();
	}

	/**
	 * The field-validation lines to display for a failed save, or an empty list when
	 * the failure is not a backend field-validation error (those general failures use
	 * the status message instead).
	 */
	public static List<String> backendValidationLines(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiRequestException request
				&& request.status() == HttpURLConnection.HTTP_BAD_REQUEST
				&& request.apiError().isPresent()) {
			ApiErrorDto error = request.apiError().get();
			if (error.validationErrors() != null && !error.validationErrors().isEmpty()) {
				return backendValidationLines(error);
			}
		}
		return List.of();
	}

	/** Safe message for a failed list/load. */
	public static String forLoadFailure(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiConnectionException) {
			return CONNECTION_UNAVAILABLE;
		}
		if (cause instanceof ApiRequestException request) {
			return generalRequest(request);
		}
		return UNEXPECTED;
	}

	/** Safe message for a failed create/update (validation-aware). */
	public static String forSaveFailure(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiConnectionException) {
			return CONNECTION_UNAVAILABLE;
		}
		if (cause instanceof ApiRequestException request) {
			return generalRequest(request);
		}
		return UNEXPECTED;
	}

	/**
	 * Every create/update failure as a list of lines for the form message area:
	 * backend field errors (HTTP 400) are aggregated one per line, while any other
	 * failure (duplicate conflict, not found, connection, unexpected) is a single
	 * safe line. All belong below the form, never above the table.
	 */
	public static List<String> saveFailureLines(Throwable throwable) {
		List<String> fieldLines = backendValidationLines(throwable);
		if (!fieldLines.isEmpty()) {
			return fieldLines;
		}
		return List.of(forSaveFailure(throwable));
	}

	/** Safe message for a failed delete (conflict-aware). */
	public static String forDeleteFailure(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiConnectionException) {
			return CONNECTION_UNAVAILABLE;
		}
		if (cause instanceof ApiRequestException request) {
			if (request.status() == HttpURLConnection.HTTP_CONFLICT) {
				return DELETE_CONFLICT;
			}
			return generalRequest(request);
		}
		return UNEXPECTED;
	}

	/**
	 * Formats backend {@code validationErrors} as one readable line per field in a
	 * deterministic order, removing duplicates. Falls back to the safe general
	 * message when no field errors are present.
	 */
	public static String backendValidation(ApiErrorDto error) {
		Map<String, String> fieldErrors = error.validationErrors();
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return error.message() == null || error.message().isBlank()
					? REQUEST_INVALID
					: error.message();
		}
		return String.join("\n", backendValidationLines(error));
	}

	/**
	 * Backend {@code validationErrors} as a list of one readable line per field, in
	 * a deterministic canonical order (unknown fields sorted after), with duplicates
	 * removed.
	 */
	public static List<String> backendValidationLines(ApiErrorDto error) {
		Map<String, String> fieldErrors = error.validationErrors();
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		for (String field : FIELD_ORDER) {
			if (fieldErrors.containsKey(field)) {
				lines.add(line(field, fieldErrors.get(field)));
			}
		}
		fieldErrors.entrySet().stream()
				.filter(entry -> !FIELD_ORDER.contains(entry.getKey()))
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> lines.add(line(entry.getKey(), entry.getValue())));
		return lines.stream().distinct().toList();
	}

	private static String generalRequest(ApiRequestException request) {
		int status = request.status();
		if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
			return request.apiError().map(CustomerMessages::backendValidation).orElse(REQUEST_INVALID);
		}
		if (status == HttpURLConnection.HTTP_UNAUTHORIZED
				|| status == HttpURLConnection.HTTP_FORBIDDEN) {
			return NOT_AUTHORIZED;
		}
		if (status == HttpURLConnection.HTTP_NOT_FOUND) {
			return NOT_FOUND;
		}
		if (status == HttpURLConnection.HTTP_CONFLICT) {
			return request.apiError()
					.map(ApiErrorDto::message)
					.filter(message -> message != null && !message.isBlank())
					.orElse(CONFLICT_GENERIC);
		}
		return "Unexpected response from the backend (status " + status + ").";
	}

	private static String line(String field, String message) {
		return FIELD_LABELS.getOrDefault(field, field) + ": " + message;
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}

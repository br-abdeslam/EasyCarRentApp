package be.condorcet.easycarrent.desktop.dto;

import java.util.Map;

/**
 * Client-side representation of the backend's consistent error payload.
 *
 * <p>Mirrors the backend error contract:
 * {@code timestamp, status, error, message, path, validationErrors}. The
 * {@code validationErrors} map is only present for field-validation failures and
 * may be {@code null} or empty otherwise.</p>
 *
 * @param timestamp        ISO-8601 instant the error was produced
 * @param status           HTTP status code
 * @param error            HTTP reason phrase
 * @param message          human-readable, safe error message
 * @param path             request path that produced the error
 * @param validationErrors field-to-message map for validation failures, or null
 */
public record ApiErrorDto(
		String timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> validationErrors) {
}

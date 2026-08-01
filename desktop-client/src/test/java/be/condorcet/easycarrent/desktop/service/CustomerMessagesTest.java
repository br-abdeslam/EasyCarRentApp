package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;

import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class CustomerMessagesTest {

	private ApiRequestException request(int status, Map<String, String> validationErrors) {
		ApiErrorDto error = new ApiErrorDto("2026-07-26T00:00:00Z", status, "Error", "message",
				"/api/customers", validationErrors);
		return new ApiRequestException(status, "message", error, "/api/customers");
	}

	private ApiRequestException request(int status, String message) {
		ApiErrorDto error = new ApiErrorDto("2026-07-26T00:00:00Z", status, "Error", message,
				"/api/customers", null);
		return new ApiRequestException(status, message, error, "/api/customers");
	}

	// --- local validation ------------------------------------------------------

	@Test
	void localValidationJoinsOnePerLinePreservingOrder() {
		String message = CustomerMessages.localValidation(
				List.of("First name is required.", "Email must be a valid email address."));
		assertEquals("First name is required.\nEmail must be a valid email address.", message);
	}

	@Test
	void localValidationRemovesDuplicates() {
		String message = CustomerMessages.localValidation(
				List.of("Phone number is invalid.", "Phone number is invalid."));
		assertEquals("Phone number is invalid.", message);
	}

	@Test
	void localValidationSingleError() {
		assertEquals("Address is required.",
				CustomerMessages.localValidation(List.of("Address is required.")));
	}

	// --- backend validation ----------------------------------------------------

	@Test
	void backendValidationSingleFieldWithReadableLabel() {
		Map<String, String> errors = Map.of("email", "must be a valid email address");
		String message = CustomerMessages.forSaveFailure(request(400, errors));
		assertEquals("Email: must be a valid email address", message);
	}

	@Test
	void backendValidationAggregatesEveryFieldInDeterministicOrder() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("email", "must be a valid email address");
		errors.put("firstName", "is required");
		errors.put("drivingLicenseExpiryDate", "must not be in the past");
		errors.put("phone", "must match the accepted phone format");

		String message = CustomerMessages.forSaveFailure(request(400, errors));

		// Canonical field order, not the map's insertion order.
		assertEquals(
				"First name: is required\n"
						+ "Email: must be a valid email address\n"
						+ "Phone: must match the accepted phone format\n"
						+ "Driving licence expiry date: must not be in the past",
				message);
	}

	@Test
	void backendValidationSortsUnknownFieldsAfterKnownOnes() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("zebra", "unknown b");
		errors.put("email", "must be a valid email address");
		errors.put("apple", "unknown a");

		String message = CustomerMessages.forSaveFailure(request(400, errors));

		assertEquals("Email: must be a valid email address\napple: unknown a\nzebra: unknown b",
				message);
	}

	@Test
	void backendValidationFallsBackWhenNoFieldErrors() {
		assertEquals("message", CustomerMessages.backendValidation(
				new ApiErrorDto(null, 400, "Bad Request", "message", "/api/customers", null)));
		assertEquals(CustomerMessages.REQUEST_INVALID, CustomerMessages.backendValidation(
				new ApiErrorDto(null, 400, "Bad Request", "", "/api/customers", Map.of())));
	}

	// --- failure dispatch ------------------------------------------------------

	@Test
	void deleteConflictUsesTheSafeRentalReferenceMessage() {
		assertEquals(CustomerMessages.DELETE_CONFLICT,
				CustomerMessages.forDeleteFailure(request(409, "Customer 5 cannot be deleted")));
	}

	@Test
	void saveConflictKeepsTheBackendDuplicateMessage() {
		assertEquals("A customer with that email already exists",
				CustomerMessages.forSaveFailure(
						request(409, "A customer with that email already exists")));
	}

	@Test
	void authorizationAndNotFoundAndConnectionMapToSafeMessages() {
		assertEquals(CustomerMessages.NOT_AUTHORIZED,
				CustomerMessages.forSaveFailure(request(403, Map.of())));
		assertEquals(CustomerMessages.NOT_FOUND,
				CustomerMessages.forSaveFailure(request(404, "x")));
		assertEquals(CustomerMessages.CONNECTION_UNAVAILABLE, CustomerMessages.forLoadFailure(
				new CompletionException(new ApiConnectionException("down", new ConnectException()))));
	}

	@Test
	void messagesNeverExposeRawJsonExceptionNamesOrPersonalValues() {
		Map<String, String> errors = Map.of("email", "must be a valid email address");
		String message = CustomerMessages.forSaveFailure(request(400, errors));
		assertFalse(message.contains("{"), "no raw JSON");
		assertFalse(message.contains("ApiRequestException"), "no exception-class name");
		assertFalse(message.toLowerCase().contains("authorization"), "no Authorization value");
		assertFalse(message.contains("@"), "no submitted email value");
		assertTrue(message.contains("Email:"), "uses a readable field label");
	}

	// --- line-list rendering model ---------------------------------------------

	@Test
	void localValidationLinesReturnEachErrorSeparatelyPreservingOrder() {
		List<String> lines = CustomerMessages.localValidationLines(List.of(
				"Email must be a valid email address.",
				"Phone must contain digits and may include spaces, parentheses, "
						+ "hyphens and an optional leading +."));
		assertEquals(2, lines.size());
		assertTrue(lines.get(0).contains("Email"));
		assertTrue(lines.get(1).contains("Phone"));
	}

	@Test
	void localValidationLinesRemoveDuplicates() {
		assertEquals(List.of("Address is required."),
				CustomerMessages.localValidationLines(
						List.of("Address is required.", "Address is required.")));
	}

	@Test
	void backendValidationLinesReturnOneEntryPerFieldForValidationFailure() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("email", "must be a valid email address");
		errors.put("firstName", "is required");

		List<String> lines = CustomerMessages.backendValidationLines(request(400, errors));

		assertEquals(List.of("First name: is required", "Email: must be a valid email address"),
				lines);
	}

	@Test
	void backendValidationLinesAreEmptyForNonValidationFailures() {
		assertTrue(CustomerMessages.backendValidationLines(request(409, "conflict")).isEmpty());
		assertTrue(CustomerMessages.backendValidationLines(request(403, Map.of())).isEmpty());
		assertTrue(CustomerMessages.backendValidationLines(new CompletionException(
				new ApiConnectionException("down", new ConnectException()))).isEmpty());
	}
}

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

class RentalMessagesTest {

	private ApiRequestException request(int status, Map<String, String> validationErrors) {
		ApiErrorDto error = new ApiErrorDto("2026-08-01T00:00:00Z", status, "Error", "message",
				"/api/rentals", validationErrors);
		return new ApiRequestException(status, "message", error, "/api/rentals");
	}

	private ApiRequestException request(int status, String message) {
		ApiErrorDto error = new ApiErrorDto("2026-08-01T00:00:00Z", status, "Error", message,
				"/api/rentals", null);
		return new ApiRequestException(status, message, error, "/api/rentals");
	}

	private CompletionException connectionFailure() {
		return new CompletionException(new ApiConnectionException("down", new ConnectException()));
	}

	// --- local validation ------------------------------------------------------

	@Test
	void localValidationLinesPreserveOrderAndDropDuplicates() {
		List<String> lines = RentalMessages.localValidationLines(List.of(
				"Customer is required.", "Vehicle is required.", "Customer is required."));
		assertEquals(List.of("Customer is required.", "Vehicle is required."), lines);
	}

	// --- backend field validation ----------------------------------------------

	@Test
	void backendValidationLinesUseReadableLabelsInCanonicalOrder() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("endDate", "endDate is required");
		errors.put("customerId", "customerId is required");
		errors.put("vehicleId", "vehicleId is required");

		List<String> lines = RentalMessages.backendValidationLines(request(400, errors));

		assertEquals(List.of(
				"Customer: customerId is required",
				"Vehicle: vehicleId is required",
				"End date: endDate is required"), lines);
	}

	@Test
	void backendValidationLinesAreEmptyForNonValidationFailures() {
		assertTrue(RentalMessages.backendValidationLines(request(409, "overlap")).isEmpty());
		assertTrue(RentalMessages.backendValidationLines(connectionFailure()).isEmpty());
	}

	// --- save-failure lines (all belong below the form) ------------------------

	@Test
	void saveFailureLinesAggregateBackendFieldErrors() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("startDate", "startDate is required");
		errors.put("endDate", "endDate is required");

		assertEquals(List.of("Start date: startDate is required", "End date: endDate is required"),
				RentalMessages.saveFailureLines(request(400, errors)));
	}

	@Test
	void saveFailureLinesShowASingleLineForAnOverlapConflict() {
		List<String> lines = RentalMessages.saveFailureLines(request(409,
				"The vehicle already has a planned or active rental overlapping the requested period"));
		assertEquals(List.of(
				"The vehicle already has a planned or active rental overlapping the requested period"),
				lines);
	}

	@Test
	void saveFailureLinesShowASingleSafeLineForAConnectionFailure() {
		assertEquals(List.of(RentalMessages.CONNECTION_UNAVAILABLE),
				RentalMessages.saveFailureLines(connectionFailure()));
	}

	@Test
	void saveFailureMapsAMissingReferenceToASafeMessage() {
		assertEquals(RentalMessages.REFERENCE_NOT_FOUND,
				RentalMessages.forSaveFailure(request(404, "Customer not found with id 5")));
	}

	@Test
	void saveFailureLinesAreNeverEmpty() {
		assertFalse(RentalMessages.saveFailureLines(request(500, "boom")).isEmpty());
		assertFalse(RentalMessages.saveFailureLines(request(403, Map.of())).isEmpty());
	}

	// --- load / delete / transition dispatch -----------------------------------

	@Test
	void loadFailureMapsConnectionAndAuthorization() {
		assertEquals(RentalMessages.CONNECTION_UNAVAILABLE,
				RentalMessages.forLoadFailure(connectionFailure()));
		assertEquals(RentalMessages.NOT_AUTHORIZED,
				RentalMessages.forLoadFailure(request(403, Map.of())));
	}

	@Test
	void deleteConflictKeepsTheSafeBackendMessage() {
		assertEquals("An active rental cannot be deleted",
				RentalMessages.forDeleteFailure(request(409, "An active rental cannot be deleted")));
		assertEquals(RentalMessages.RENTAL_NOT_FOUND,
				RentalMessages.forDeleteFailure(request(404, "Rental not found with id 5")));
	}

	@Test
	void transitionConflictKeepsTheSafeBackendMessage() {
		assertEquals("The vehicle must be AVAILABLE to start this rental but is MAINTENANCE",
				RentalMessages.forTransitionFailure(request(409,
						"The vehicle must be AVAILABLE to start this rental but is MAINTENANCE")));
	}

	@Test
	void messagesNeverExposeRawJsonExceptionNamesOrPasswords() {
		Map<String, String> errors = Map.of("customerId", "customerId is required");
		String message = String.join("\n", RentalMessages.saveFailureLines(request(400, errors)));
		assertFalse(message.contains("{"), "no raw JSON");
		assertFalse(message.contains("ApiRequestException"), "no exception-class name");
		assertFalse(message.toLowerCase().contains("authorization"), "no Authorization value");
		assertTrue(message.contains("Customer:"), "uses a readable field label");
	}
}

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

class MaintenanceMessagesTest {

	private ApiRequestException request(int status, Map<String, String> validationErrors) {
		ApiErrorDto error = new ApiErrorDto("2026-08-04T00:00:00Z", status, "Error", "message",
				"/api/maintenance-records", validationErrors);
		return new ApiRequestException(status, "message", error, "/api/maintenance-records");
	}

	private ApiRequestException request(int status, String message) {
		ApiErrorDto error = new ApiErrorDto("2026-08-04T00:00:00Z", status, "Error", message,
				"/api/maintenance-records", null);
		return new ApiRequestException(status, message, error, "/api/maintenance-records");
	}

	private CompletionException connectionFailure() {
		return new CompletionException(new ApiConnectionException("down", new ConnectException()));
	}

	@Test
	void localValidationLinesPreserveOrderAndDropDuplicates() {
		List<String> lines = MaintenanceMessages.localValidationLines(List.of(
				"Vehicle is required.", "Cost is required.", "Vehicle is required."));
		assertEquals(List.of("Vehicle is required.", "Cost is required."), lines);
	}

	@Test
	void backendValidationLinesUseReadableLabelsInCanonicalOrder() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("cost", "cost is required");
		errors.put("description", "description is required");
		errors.put("vehicleId", "vehicleId is required");

		List<String> lines = MaintenanceMessages.backendValidationLines(request(400, errors));

		assertEquals(List.of(
				"Vehicle: vehicleId is required",
				"Description: description is required",
				"Cost: cost is required"), lines);
	}

	@Test
	void saveFailureLinesAggregateBackendFieldErrors() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("startDate", "startDate is required");
		errors.put("endDate", "endDate is required");

		assertEquals(List.of("Start date: startDate is required", "End date: endDate is required"),
				MaintenanceMessages.saveFailureLines(request(400, errors)));
	}

	@Test
	void saveFailureLinesShowASingleLineForAnOverlapConflict() {
		List<String> lines = MaintenanceMessages.saveFailureLines(request(409,
				"Vehicle 4 already has maintenance scheduled overlapping 2027-09-01 to 2027-09-03"));
		assertEquals(List.of(
				"Vehicle 4 already has maintenance scheduled overlapping 2027-09-01 to 2027-09-03"),
				lines);
	}

	@Test
	void saveFailureLinesShowASingleLineForADateOrderError() {
		List<String> lines = MaintenanceMessages.saveFailureLines(
				request(400, "Maintenance end date must be on or after start date"));
		assertEquals(List.of("Maintenance end date must be on or after start date"), lines);
	}

	@Test
	void saveFailureLinesShowASingleSafeLineForAConnectionFailure() {
		assertEquals(List.of(MaintenanceMessages.CONNECTION_UNAVAILABLE),
				MaintenanceMessages.saveFailureLines(connectionFailure()));
	}

	@Test
	void saveFailureMapsAMissingVehicleToASafeMessage() {
		assertEquals(MaintenanceMessages.VEHICLE_NOT_FOUND,
				MaintenanceMessages.forSaveFailure(request(404, "Vehicle not found with id: 4")));
	}

	@Test
	void loadFailureMapsConnectionAndAuthorization() {
		assertEquals(MaintenanceMessages.CONNECTION_UNAVAILABLE,
				MaintenanceMessages.forLoadFailure(connectionFailure()));
		assertEquals(MaintenanceMessages.NOT_AUTHORIZED,
				MaintenanceMessages.forLoadFailure(request(403, Map.of())));
	}

	@Test
	void deleteConflictKeepsTheSafeBackendMessage() {
		assertEquals("Maintenance record 6 is IN_PROGRESS and can only be deleted while PLANNED",
				MaintenanceMessages.forDeleteFailure(request(409,
						"Maintenance record 6 is IN_PROGRESS and can only be deleted while PLANNED")));
		assertEquals(MaintenanceMessages.RECORD_NOT_FOUND,
				MaintenanceMessages.forDeleteFailure(request(404, "Maintenance record not found with id: 6")));
	}

	@Test
	void transitionConflictKeepsTheSafeBackendMessage() {
		assertEquals("Maintenance record 6 can only be started from PLANNED but is IN_PROGRESS",
				MaintenanceMessages.forTransitionFailure(request(409,
						"Maintenance record 6 can only be started from PLANNED but is IN_PROGRESS")));
	}

	@Test
	void messagesNeverExposeRawJsonExceptionNamesOrPasswords() {
		Map<String, String> errors = Map.of("vehicleId", "vehicleId is required");
		String message = String.join("\n", MaintenanceMessages.saveFailureLines(request(400, errors)));
		assertFalse(message.contains("{"), "no raw JSON");
		assertFalse(message.contains("ApiRequestException"), "no exception-class name");
		assertFalse(message.toLowerCase().contains("authorization"), "no Authorization value");
		assertTrue(message.contains("Vehicle:"), "uses a readable field label");
	}
}

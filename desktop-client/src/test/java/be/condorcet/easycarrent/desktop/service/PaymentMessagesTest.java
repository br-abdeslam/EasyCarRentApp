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

class PaymentMessagesTest {

	private ApiRequestException request(int status, Map<String, String> validationErrors) {
		ApiErrorDto error = new ApiErrorDto("2026-08-01T00:00:00Z", status, "Error", "message",
				"/api/payments", validationErrors);
		return new ApiRequestException(status, "message", error, "/api/payments");
	}

	private ApiRequestException request(int status, String message) {
		ApiErrorDto error = new ApiErrorDto("2026-08-01T00:00:00Z", status, "Error", message,
				"/api/payments", null);
		return new ApiRequestException(status, message, error, "/api/payments");
	}

	private CompletionException connectionFailure() {
		return new CompletionException(new ApiConnectionException("down", new ConnectException()));
	}

	@Test
	void localValidationLinesPreserveOrderAndDropDuplicates() {
		List<String> lines = PaymentMessages.localValidationLines(List.of(
				"Rental is required.", "Payment method is required.", "Rental is required."));
		assertEquals(List.of("Rental is required.", "Payment method is required."), lines);
	}

	@Test
	void backendValidationLinesUseReadableLabelsInCanonicalOrder() {
		Map<String, String> errors = new LinkedHashMap<>();
		errors.put("paymentMethod", "paymentMethod is required");
		errors.put("rentalId", "rentalId is required");

		List<String> lines = PaymentMessages.backendValidationLines(request(400, errors));

		assertEquals(List.of(
				"Rental: rentalId is required",
				"Payment method: paymentMethod is required"), lines);
	}

	@Test
	void saveFailureLinesShowASingleLineForADuplicateConflict() {
		List<String> lines = PaymentMessages.saveFailureLines(
				request(409, "A payment already exists for rental 4"));
		assertEquals(List.of("A payment already exists for rental 4"), lines);
	}

	@Test
	void saveFailureLinesShowASingleLineForANonPayableRentalConflict() {
		List<String> lines = PaymentMessages.saveFailureLines(request(409,
				"A payment can only be created for an ACTIVE or COMPLETED rental but rental 4 is PLANNED"));
		assertEquals(List.of(
				"A payment can only be created for an ACTIVE or COMPLETED rental but rental 4 is PLANNED"),
				lines);
	}

	@Test
	void saveFailureLinesShowASingleSafeLineForAConnectionFailure() {
		assertEquals(List.of(PaymentMessages.CONNECTION_UNAVAILABLE),
				PaymentMessages.saveFailureLines(connectionFailure()));
	}

	@Test
	void saveFailureMapsAMissingRentalToASafeMessage() {
		assertEquals(PaymentMessages.RENTAL_NOT_FOUND,
				PaymentMessages.forSaveFailure(request(404, "Rental not found with id: 4")));
	}

	@Test
	void saveFailureLinesAreNeverEmpty() {
		assertFalse(PaymentMessages.saveFailureLines(request(500, "boom")).isEmpty());
		assertFalse(PaymentMessages.saveFailureLines(request(403, Map.of())).isEmpty());
	}

	@Test
	void loadFailureMapsConnectionAndAuthorization() {
		assertEquals(PaymentMessages.CONNECTION_UNAVAILABLE,
				PaymentMessages.forLoadFailure(connectionFailure()));
		assertEquals(PaymentMessages.NOT_AUTHORIZED,
				PaymentMessages.forLoadFailure(request(403, Map.of())));
	}

	@Test
	void deleteConflictKeepsTheSafeBackendMessage() {
		assertEquals("A payment that is PAID cannot be deleted",
				PaymentMessages.forDeleteFailure(request(409, "A payment that is PAID cannot be deleted")));
		assertEquals(PaymentMessages.PAYMENT_NOT_FOUND,
				PaymentMessages.forDeleteFailure(request(404, "Payment not found with id: 8")));
	}

	@Test
	void transitionConflictKeepsTheSafeBackendMessage() {
		assertEquals("A payment can only be refunded from PAID but payment 8 is PENDING",
				PaymentMessages.forTransitionFailure(request(409,
						"A payment can only be refunded from PAID but payment 8 is PENDING")));
	}

	@Test
	void messagesNeverExposeRawJsonExceptionNamesOrPasswords() {
		Map<String, String> errors = Map.of("rentalId", "rentalId is required");
		String message = String.join("\n", PaymentMessages.saveFailureLines(request(400, errors)));
		assertFalse(message.contains("{"), "no raw JSON");
		assertFalse(message.contains("ApiRequestException"), "no exception-class name");
		assertFalse(message.toLowerCase().contains("authorization"), "no Authorization value");
		assertTrue(message.contains("Rental:"), "uses a readable field label");
	}
}

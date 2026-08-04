package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentRequestDto;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class PaymentServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private static final String SAMPLE_JSON = """
			{"id":8,"rentalId":4,"rentalStatus":"ACTIVE","amount":135.00,"paymentMethod":"CARD",
			 "status":"PENDING","createdAt":"2026-08-01T10:15:30","paidAt":null}
			""";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private PaymentService service(RecordingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new PaymentService(apiClient, session);
	}

	private PaymentRequestDto sampleRequest() {
		return new PaymentRequestDto(4L, PaymentMethod.CARD);
	}

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http =
				RecordingHttpClient.returning(new FakeHttpResponse(200, "[" + SAMPLE_JSON + "]"));

		List<PaymentResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(1, result.size());
		assertEquals(PaymentStatus.PENDING, result.get(0).status());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));
		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void findByRentalUsesRentalPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).findByRental(4).join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/rental/4", http.lastRequest().uri().toString());
	}

	@Test
	void createPostsRentalAndMethodAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201, SAMPLE_JSON));

		PaymentResponseDto created = service(http, authenticatedSession()).create(sampleRequest()).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments", http.lastRequest().uri().toString());
		assertTrue(http.lastRequestBody().contains("\"rentalId\":4"));
		assertTrue(http.lastRequestBody().contains("\"paymentMethod\":\"CARD\""));
		assertFalse(http.lastRequestBody().contains("\"amount\""));
		assertFalse(http.lastRequestBody().contains("\"status\""));
		assertEquals(8L, created.id());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(8).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/8", http.lastRequest().uri().toString());
	}

	@Test
	void markPaidUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).markPaid(8).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/8/pay", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
	}

	@Test
	void markFailedUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));
		service(http, authenticatedSession()).markFailed(8).join();
		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/8/fail", http.lastRequest().uri().toString());
	}

	@Test
	void retryUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));
		service(http, authenticatedSession()).retry(8).join();
		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/8/retry", http.lastRequest().uri().toString());
	}

	@Test
	void refundUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));
		service(http, authenticatedSession()).refund(8).join();
		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/payments/8/refund", http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		PaymentService service = service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
				new SessionManager());
		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAndInvalidIdsAreRejected() {
		PaymentService service = service(RecordingHttpClient.returning(new FakeHttpResponse(204, "")),
				authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
		assertThrows(IllegalArgumentException.class, () -> service.findByRental(0));
		assertThrows(IllegalArgumentException.class, () -> service.markPaid(0));
		assertThrows(IllegalArgumentException.class, () -> service.markFailed(-1));
		assertThrows(IllegalArgumentException.class, () -> service.retry(0));
		assertThrows(IllegalArgumentException.class, () -> service.refund(0));
	}

	@Test
	void apiRequestExceptionIsPreservedWithoutCredentialLeak() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"duplicate\","
						+ "\"path\":\"/api/payments\"}"));

		CompletionException thrown = assertThrows(CompletionException.class,
				() -> service(http, authenticatedSession()).create(sampleRequest()).join());

		ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
		assertEquals(409, ex.status());
		assertFalse(ex.getMessage().contains(PASSWORD));
	}

	@Test
	void apiConnectionExceptionIsPreserved() {
		RecordingHttpClient http =
				RecordingHttpClient.failingWith(new ConnectException("Connection refused"));

		CompletionException thrown = assertThrows(CompletionException.class,
				() -> service(http, authenticatedSession()).findAll().join());

		ApiConnectionException ex = assertInstanceOf(ApiConnectionException.class, thrown.getCause());
		assertFalse(ex.getMessage().contains(PASSWORD));
	}
}

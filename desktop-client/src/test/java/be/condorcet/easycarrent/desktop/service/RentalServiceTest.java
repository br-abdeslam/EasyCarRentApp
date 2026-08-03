package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.RentalRequestDto;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
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
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class RentalServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private static final String SAMPLE_JSON = """
			{"id":9,"startDate":"2026-09-01","endDate":"2026-09-04","status":"PLANNED",
			 "totalPrice":135.00,"createdAt":"2026-08-01T10:15:30","vehicleId":4,
			 "vehicleRegistrationNumber":"TEST-REG-001","vehicleBrand":"TestBrand",
			 "vehicleModel":"TestModel","customerId":3,"customerFirstName":"Test",
			 "customerLastName":"Customer"}
			""";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private RentalService service(RecordingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new RentalService(apiClient, session);
	}

	private RentalRequestDto sampleRequest() {
		return new RentalRequestDto(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 4), 4L, 3L);
	}

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http =
				RecordingHttpClient.returning(new FakeHttpResponse(200, "[" + SAMPLE_JSON + "]"));

		List<RentalResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(1, result.size());
		assertEquals(RentalStatus.PLANNED, result.get(0).status());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));

		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void createPostsPeriodAndReferencesAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201, SAMPLE_JSON));

		RentalResponseDto created = service(http, authenticatedSession()).create(sampleRequest()).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals", http.lastRequest().uri().toString());
		assertTrue(http.lastRequestBody().contains("\"startDate\":\"2026-09-01\""));
		assertTrue(http.lastRequestBody().contains("\"vehicleId\":4"));
		assertFalse(http.lastRequestBody().contains("\"status\""));
		assertFalse(http.lastRequestBody().contains("\"totalPrice\""));
		assertEquals(9L, created.id());
	}

	@Test
	void updateUsesIdPathAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).update(5, sampleRequest()).join();

		assertEquals("PUT", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals/5", http.lastRequest().uri().toString());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(5).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals/5", http.lastRequest().uri().toString());
	}

	@Test
	void startUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).start(5).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals/5/start", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
	}

	@Test
	void completeUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).complete(5).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals/5/complete", http.lastRequest().uri().toString());
	}

	@Test
	void cancelUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).cancel(5).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/rentals/5/cancel", http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		RentalService service = service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
				new SessionManager());

		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAndInvalidIdsAreRejected() {
		RentalService service = service(RecordingHttpClient.returning(new FakeHttpResponse(204, "")),
				authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(NullPointerException.class, () -> service.update(1, null));
		assertThrows(IllegalArgumentException.class, () -> service.update(0, sampleRequest()));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
		assertThrows(IllegalArgumentException.class, () -> service.start(0));
		assertThrows(IllegalArgumentException.class, () -> service.complete(-1));
		assertThrows(IllegalArgumentException.class, () -> service.cancel(0));
	}

	@Test
	void apiRequestExceptionIsPreservedWithoutCredentialLeak() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"overlap\","
						+ "\"path\":\"/api/rentals\"}"));

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

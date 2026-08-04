package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.MaintenanceRequestDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;
import be.condorcet.easycarrent.desktop.session.SessionManager;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class MaintenanceServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private static final String SAMPLE_JSON = """
			{"id":6,"vehicleId":4,"description":"Scheduled brake inspection",
			 "startDate":"2027-09-01","endDate":"2027-09-03","cost":180.00,"status":"PLANNED"}
			""";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private MaintenanceService service(RecordingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new MaintenanceService(apiClient, session);
	}

	private MaintenanceRequestDto sampleRequest() {
		return new MaintenanceRequestDto(4L, "Scheduled brake inspection",
				LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3), new BigDecimal("180.00"));
	}

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http =
				RecordingHttpClient.returning(new FakeHttpResponse(200, "[" + SAMPLE_JSON + "]"));

		List<MaintenanceResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/maintenance-records",
				http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(1, result.size());
		assertEquals(MaintenanceStatus.PLANNED, result.get(0).status());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));
		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void createPostsRequestBodyAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201, SAMPLE_JSON));

		MaintenanceResponseDto created =
				service(http, authenticatedSession()).create(sampleRequest()).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/maintenance-records",
				http.lastRequest().uri().toString());
		assertTrue(http.lastRequestBody().contains("\"vehicleId\":4"));
		assertTrue(http.lastRequestBody().contains("\"cost\":180.00"));
		assertFalse(http.lastRequestBody().contains("\"status\""));
		assertEquals(6L, created.id());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(6).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/maintenance-records/6",
				http.lastRequest().uri().toString());
	}

	@Test
	void startUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).start(6).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/maintenance-records/6/start",
				http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
	}

	@Test
	void completeUsesDedicatedPatchTransitionEndpoint() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).complete(6).join();

		assertEquals("PATCH", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/maintenance-records/6/complete",
				http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		MaintenanceService service = service(
				RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")), new SessionManager());
		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAndInvalidIdsAreRejected() {
		MaintenanceService service = service(
				RecordingHttpClient.returning(new FakeHttpResponse(204, "")), authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
		assertThrows(IllegalArgumentException.class, () -> service.start(0));
		assertThrows(IllegalArgumentException.class, () -> service.complete(-1));
	}

	@Test
	void apiRequestExceptionIsPreservedWithoutCredentialLeak() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"overlap\","
						+ "\"path\":\"/api/maintenance-records\"}"));

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

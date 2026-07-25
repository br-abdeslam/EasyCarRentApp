package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.VehicleRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
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
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class VehicleServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private VehicleService service(RecordingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new VehicleService(apiClient, session);
	}

	private VehicleRequestDto sampleRequest() {
		return new VehicleRequestDto("1-ABC-123", "Toyota", "Yaris", 2022, "Blue",
				new BigDecimal("42.50"), 15000L, 3L);
	}

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"[{\"id\":1,\"registrationNumber\":\"A\",\"brand\":\"B\",\"model\":\"M\","
						+ "\"dailyPrice\":9.99,\"status\":\"AVAILABLE\",\"categoryId\":1,"
						+ "\"categoryName\":\"Compact\"}]"));

		List<VehicleResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/vehicles", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(1, result.size());
		assertEquals("Compact", result.get(0).categoryName());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));

		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void createPostsRequestBodyAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201,
				"{\"id\":9,\"registrationNumber\":\"1-ABC-123\",\"brand\":\"Toyota\",\"model\":\"Yaris\","
						+ "\"dailyPrice\":42.50,\"status\":\"AVAILABLE\",\"categoryId\":3,"
						+ "\"categoryName\":\"Compact\"}"));

		VehicleResponseDto created =
				service(http, authenticatedSession()).create(sampleRequest()).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/vehicles", http.lastRequest().uri().toString());
		assertEquals("application/json",
				http.lastRequest().headers().firstValue("Content-Type").orElseThrow());
		assertTrue(http.lastRequestBody().contains("\"registrationNumber\":\"1-ABC-123\""));
		assertTrue(http.lastRequestBody().contains("\"categoryId\":3"));
		assertFalse(http.lastRequestBody().contains("\"status\""));
		assertEquals(9L, created.id());
	}

	@Test
	void updateUsesIdPathAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"{\"id\":5,\"registrationNumber\":\"R\",\"brand\":\"B\",\"model\":\"M\","
						+ "\"dailyPrice\":10.00,\"status\":\"AVAILABLE\",\"categoryId\":1,"
						+ "\"categoryName\":\"C\"}"));

		VehicleResponseDto updated =
				service(http, authenticatedSession()).update(5, sampleRequest()).join();

		assertEquals("PUT", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/vehicles/5", http.lastRequest().uri().toString());
		assertEquals(5L, updated.id());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(5).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/vehicles/5", http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		VehicleService service = service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
				new SessionManager());

		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAndInvalidIdsAreRejected() {
		VehicleService service = service(RecordingHttpClient.returning(new FakeHttpResponse(204, "")),
				authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(NullPointerException.class, () -> service.update(1, null));
		assertThrows(IllegalArgumentException.class, () -> service.update(0, sampleRequest()));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
	}

	@Test
	void apiRequestExceptionIsPreserved() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"already exists\","
						+ "\"path\":\"/api/vehicles\"}"));

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

		ApiConnectionException ex =
				assertInstanceOf(ApiConnectionException.class, thrown.getCause());
		assertFalse(ex.getMessage().contains(PASSWORD));
	}
}

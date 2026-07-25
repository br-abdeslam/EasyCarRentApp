package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
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

class VehicleCategoryServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private ApiClient apiClient(RecordingHttpClient http) {
		return new ApiClient(BASE_URI, http, JsonMapperFactory.create(), Duration.ofSeconds(10));
	}

	private VehicleCategoryService service(RecordingHttpClient http, SessionManager session) {
		return new VehicleCategoryService(apiClient(http), session);
	}

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"[{\"id\":1,\"name\":\"Compact\",\"description\":\"a\"},"
						+ "{\"id\":2,\"name\":\"SUV\",\"description\":\"b\"}]"));

		List<VehicleCategoryResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(2, result.size());
		assertEquals("Compact", result.get(0).name());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));

		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void createPostsRequestBodyAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201,
				"{\"id\":9,\"name\":\"Compact\",\"description\":\"Small\"}"));

		VehicleCategoryResponseDto created = service(http, authenticatedSession())
				.create(new VehicleCategoryRequestDto("Compact", "Small")).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories", http.lastRequest().uri().toString());
		assertEquals("application/json",
				http.lastRequest().headers().firstValue("Content-Type").orElseThrow());
		assertTrue(http.lastRequestBody().contains("\"name\":\"Compact\""));
		assertEquals(9L, created.id());
	}

	@Test
	void updateUsesIdPathAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"{\"id\":5,\"name\":\"Renamed\",\"description\":null}"));

		VehicleCategoryResponseDto updated = service(http, authenticatedSession())
				.update(5, new VehicleCategoryRequestDto("Renamed", null)).join();

		assertEquals("PUT", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories/5", http.lastRequest().uri().toString());
		assertEquals("Renamed", updated.name());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(5).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories/5", http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		VehicleCategoryService service =
				service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
						new SessionManager());

		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAreRejected() {
		VehicleCategoryService service =
				service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
						authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(NullPointerException.class, () -> service.update(1, null));
	}

	@Test
	void nonPositiveIdIsRejected() {
		VehicleCategoryService service =
				service(RecordingHttpClient.returning(new FakeHttpResponse(204, "")),
						authenticatedSession());

		assertThrows(IllegalArgumentException.class,
				() -> service.update(0, new VehicleCategoryRequestDto("x", null)));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
	}

	@Test
	void apiRequestExceptionIsPreserved() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"already exists\","
						+ "\"path\":\"/api/categories\"}"));

		CompletionException thrown = assertThrows(CompletionException.class,
				() -> service(http, authenticatedSession())
						.create(new VehicleCategoryRequestDto("Compact", null)).join());

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

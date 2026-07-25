package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;
import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
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

class CustomerServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private SessionManager authenticatedSession() {
		SessionManager session = new SessionManager();
		session.start(new AuthenticatedUser(USERNAME, DesktopUserRole.ADMIN),
				new BasicCredentials(USERNAME, PASSWORD));
		return session;
	}

	private CustomerService service(RecordingHttpClient http, SessionManager session) {
		ApiClient apiClient = new ApiClient(BASE_URI, http, JsonMapperFactory.create(),
				Duration.ofSeconds(10));
		return new CustomerService(apiClient, session);
	}

	private CustomerRequestDto sampleRequest() {
		return new CustomerRequestDto("Test", "Customer", "test.customer@example.invalid",
				"+0000000000", "1 Example Street", "TEST-LICENCE-001", LocalDate.of(2030, 1, 15));
	}

	private static final String SAMPLE_JSON =
			"{\"id\":9,\"firstName\":\"Test\",\"lastName\":\"Customer\","
					+ "\"email\":\"test.customer@example.invalid\",\"phone\":\"+0000000000\","
					+ "\"address\":\"1 Example Street\",\"drivingLicenseNumber\":\"TEST-LICENCE-001\","
					+ "\"drivingLicenseExpiryDate\":\"2030-01-15\"}";

	@Test
	void findAllUsesCollectionEndpointWithCredentialsAndMapsResult() {
		RecordingHttpClient http =
				RecordingHttpClient.returning(new FakeHttpResponse(200, "[" + SAMPLE_JSON + "]"));

		List<CustomerResponseDto> result = service(http, authenticatedSession()).findAll().join();

		assertEquals("GET", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/customers", http.lastRequest().uri().toString());
		assertEquals(new BasicCredentials(USERNAME, PASSWORD).toAuthorizationHeader(),
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals(1, result.size());
		assertEquals(LocalDate.of(2030, 1, 15), result.get(0).drivingLicenseExpiryDate());
	}

	@Test
	void findAllReturnsEmptyListForEmptyBackend() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));

		assertTrue(service(http, authenticatedSession()).findAll().join().isEmpty());
	}

	@Test
	void createPostsRequestBodyAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201, SAMPLE_JSON));

		CustomerResponseDto created =
				service(http, authenticatedSession()).create(sampleRequest()).join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/customers", http.lastRequest().uri().toString());
		assertEquals("application/json",
				http.lastRequest().headers().firstValue("Content-Type").orElseThrow());
		assertTrue(http.lastRequestBody().contains("\"drivingLicenseExpiryDate\":\"2030-01-15\""));
		assertFalse(http.lastRequestBody().contains("\"id\""));
		assertEquals(9L, created.id());
	}

	@Test
	void updateUsesIdPathAndReturnsResponse() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, SAMPLE_JSON));

		service(http, authenticatedSession()).update(5, sampleRequest()).join();

		assertEquals("PUT", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/customers/5", http.lastRequest().uri().toString());
	}

	@Test
	void deleteUsesIdPath() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		service(http, authenticatedSession()).delete(5).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/customers/5", http.lastRequest().uri().toString());
	}

	@Test
	void unauthenticatedSessionRejectsOperations() {
		CustomerService service = service(RecordingHttpClient.returning(new FakeHttpResponse(200, "[]")),
				new SessionManager());

		assertThrows(IllegalStateException.class, service::findAll);
	}

	@Test
	void nullRequestsAndInvalidIdsAreRejected() {
		CustomerService service = service(RecordingHttpClient.returning(new FakeHttpResponse(204, "")),
				authenticatedSession());

		assertThrows(NullPointerException.class, () -> service.create(null));
		assertThrows(NullPointerException.class, () -> service.update(1, null));
		assertThrows(IllegalArgumentException.class, () -> service.update(0, sampleRequest()));
		assertThrows(IllegalArgumentException.class, () -> service.delete(0));
	}

	@Test
	void apiRequestExceptionIsPreservedWithoutCredentialLeak() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(409,
				"{\"status\":409,\"error\":\"Conflict\",\"message\":\"conflict\","
						+ "\"path\":\"/api/customers\"}"));

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

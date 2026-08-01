package be.condorcet.easycarrent.desktop.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class ApiClientTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final Duration TIMEOUT = Duration.ofSeconds(7);
	private static final String PING_PATH = "/api/ping";
	private static final String PING_BODY = "Easy Car Rent API is running";
	private static final String VEHICLES_PATH = "/api/vehicles";
	private static final String TEST_USERNAME = "test-user";
	private static final String TEST_PASSWORD = "fictional-password";

	private ApiClient client(RecordingHttpClient http) {
		return new ApiClient(BASE_URI, http, JsonMapperFactory.create(), TIMEOUT);
	}

	private static BasicCredentials testCredentials() {
		return new BasicCredentials(TEST_USERNAME, TEST_PASSWORD);
	}

	@Test
	void textGetResolvesUriUsesGetAndSetsTimeout() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, PING_BODY));

		client(http).getText(PING_PATH).join();

		assertEquals("http://localhost:8080/api/ping", http.lastRequest().uri().toString());
		assertEquals("GET", http.lastRequest().method());
		assertTrue(http.lastRequest().timeout().isPresent(), "request timeout must be set");
		assertEquals(TIMEOUT, http.lastRequest().timeout().orElseThrow());
	}

	@Test
	void textGetSendsNoAuthorizationHeader() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, PING_BODY));

		client(http).getText(PING_PATH).join();

		assertTrue(http.lastRequest().headers().firstValue("Authorization").isEmpty(),
				"no Authorization header must be sent in this milestone");
	}

	@Test
	void successfulTextBodyIsReturned() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, PING_BODY));

		assertEquals(PING_BODY, client(http).getText(PING_PATH).join());
	}

	@Test
	void noContentReturnsEmptyText() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		assertEquals("", client(http).getText("/api/empty").join());
	}

	@Test
	void typedJsonBodyIsDeserialized() {
		String json = """
				{"timestamp":"2026-07-25T10:15:30Z","status":200,"error":"OK",
				 "message":"ready","path":"/api/status"}
				""";
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, json));

		ApiErrorDto dto = client(http).getJson("/api/status", ApiErrorDto.class).join();

		assertEquals(200, dto.status());
		assertEquals("ready", dto.message());
	}

	@Test
	void nonSuccessBecomesApiRequestExceptionPreservingFields() {
		String json = """
				{"timestamp":"2026-07-25T10:15:30Z","status":400,"error":"Bad Request",
				 "message":"Validation failed for one or more fields","path":"/api/vehicles",
				 "validationErrors":{"registrationNumber":"must not be blank"}}
				""";
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(400, json));

		CompletableFuture<String> future = client(http).getText("/api/vehicles");
		CompletionException thrown = assertThrows(CompletionException.class, future::join);

		ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
		assertEquals(400, ex.status());
		assertEquals("/api/vehicles", ex.path());
		assertTrue(ex.apiError().isPresent());
		assertEquals("Validation failed for one or more fields", ex.getMessage());
		assertEquals("must not be blank",
				ex.apiError().orElseThrow().validationErrors().get("registrationNumber"));
	}

	@Test
	void malformedErrorBodyStillBecomesSafeApiRequestException() {
		RecordingHttpClient http =
				RecordingHttpClient.returning(new FakeHttpResponse(400, "this is not valid json {"));

		CompletableFuture<String> future = client(http).getText("/api/vehicles");
		CompletionException thrown = assertThrows(CompletionException.class, future::join);

		ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
		assertEquals(400, ex.status());
		assertTrue(ex.apiError().isEmpty(), "malformed error body must not yield an ApiErrorDto");
		assertTrue(ex.getMessage().contains("400"), "fallback message should mention the status");
	}

	@Test
	void transportFailureBecomesApiConnectionException() {
		RecordingHttpClient http =
				RecordingHttpClient.failingWith(new ConnectException("Connection refused"));

		CompletionException thrown =
				assertThrows(CompletionException.class, () -> client(http).getText(PING_PATH).join());

		assertInstanceOf(ApiConnectionException.class, thrown.getCause());
	}

	@Test
	void getReturnsCompletableFutureWithoutBlocking() {
		CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
		RecordingHttpClient http = RecordingHttpClient.deferred(pending);

		CompletableFuture<String> future = client(http).getText(PING_PATH);

		assertFalse(future.isDone(), "sendAsync must not block until the response is available");
		pending.complete(new FakeHttpResponse(200, PING_BODY));
		assertEquals(PING_BODY, future.join());
	}

	@Test
	void authenticatedGetSendsBasicAuthorizationHeader() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));
		String expectedHeader = "Basic " + Base64.getEncoder().encodeToString(
				(TEST_USERNAME + ":" + TEST_PASSWORD).getBytes(StandardCharsets.UTF_8));

		client(http).getText(VEHICLES_PATH, testCredentials()).join();

		assertEquals(expectedHeader,
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
	}

	@Test
	void authenticatedGetReturnsBody() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200, "[]"));

		assertEquals("[]", client(http).getText(VEHICLES_PATH, testCredentials()).join());
	}

	@Test
	void authenticatedUnauthorizedBecomesApiRequestExceptionStatus401() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(401, ""));

		CompletableFuture<String> future = client(http).getText(VEHICLES_PATH, testCredentials());
		CompletionException thrown = assertThrows(CompletionException.class, future::join);

		ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
		assertEquals(401, ex.status());
		assertFalse(ex.getMessage().contains(TEST_PASSWORD),
				"exception message must not expose the password");
	}

	@Test
	void authenticatedTransportFailureDoesNotExposeCredentials() {
		RecordingHttpClient http =
				RecordingHttpClient.failingWith(new ConnectException("Connection refused"));

		CompletableFuture<String> future = client(http).getText(VEHICLES_PATH, testCredentials());
		CompletionException thrown = assertThrows(CompletionException.class, future::join);

		ApiConnectionException ex =
				assertInstanceOf(ApiConnectionException.class, thrown.getCause());
		assertFalse(ex.getMessage().contains(TEST_PASSWORD),
				"exception message must not expose the password");
	}

	// --- Generic authenticated JSON operations ---------------------------------

	private static final String EXPECTED_AUTH = "Basic " + Base64.getEncoder().encodeToString(
			(TEST_USERNAME + ":" + TEST_PASSWORD).getBytes(StandardCharsets.UTF_8));

	@Test
	void authenticatedJsonGetSendsBasicAuthorizationAndReturnsTyped() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"{\"status\":200,\"message\":\"ok\"}"));

		ApiErrorDto dto = client(http).getJson(VEHICLES_PATH, ApiErrorDto.class, testCredentials()).join();

		assertEquals(EXPECTED_AUTH,
				http.lastRequest().headers().firstValue("Authorization").orElseThrow());
		assertEquals("ok", dto.message());
	}

	@Test
	void authenticatedJsonListDeserializes() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"[{\"status\":200,\"message\":\"a\"},{\"status\":200,\"message\":\"b\"}]"));

		List<ApiErrorDto> list =
				client(http).getJsonList(VEHICLES_PATH, ApiErrorDto.class, testCredentials()).join();

		assertEquals(2, list.size());
		assertEquals("a", list.get(0).message());
	}

	@Test
	void postSendsPostMethodJsonContentTypeAndBody() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201,
				"{\"status\":201,\"message\":\"created\"}"));

		ApiErrorDto dto = client(http)
				.postJson(VEHICLES_PATH, Map.of("name", "Compact"), ApiErrorDto.class, testCredentials())
				.join();

		assertEquals("POST", http.lastRequest().method());
		assertEquals("application/json",
				http.lastRequest().headers().firstValue("Content-Type").orElseThrow());
		assertTrue(http.lastRequestBody().contains("\"name\":\"Compact\""));
		assertEquals("created", dto.message());
	}

	@Test
	void putUsesPutMethodResolvedUriAndBody() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"{\"status\":200,\"message\":\"updated\"}"));

		client(http).putJson("/api/categories/5", Map.of("name", "Renamed"), ApiErrorDto.class,
				testCredentials()).join();

		assertEquals("PUT", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories/5", http.lastRequest().uri().toString());
		assertTrue(http.lastRequestBody().contains("\"name\":\"Renamed\""));
	}

	@Test
	void deleteUsesDeleteMethodAndHandlesNoContent() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(204, ""));

		client(http).delete("/api/categories/5", testCredentials()).join();

		assertEquals("DELETE", http.lastRequest().method());
		assertEquals("http://localhost:8080/api/categories/5", http.lastRequest().uri().toString());
	}

	@Test
	void validationResponsePreservesApiErrorDto() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(400,
				"{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed\","
						+ "\"path\":\"/api/categories\",\"validationErrors\":{\"name\":\"is required\"}}"));

		CompletableFuture<ApiErrorDto> future =
				client(http).postJson("/api/categories", Map.of(), ApiErrorDto.class, testCredentials());
		CompletionException thrown = assertThrows(CompletionException.class, future::join);

		ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
		assertEquals(400, ex.status());
		assertEquals("is required",
				ex.apiError().orElseThrow().validationErrors().get("name"));
	}

	@Test
	void errorStatusesRemainApiRequestException() {
		for (int status : new int[] {401, 403, 404, 409}) {
			RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(status, ""));

			CompletableFuture<ApiErrorDto> future =
					client(http).getJson("/api/categories", ApiErrorDto.class, testCredentials());
			CompletionException thrown = assertThrows(CompletionException.class, future::join);

			ApiRequestException ex = assertInstanceOf(ApiRequestException.class, thrown.getCause());
			assertEquals(status, ex.status());
		}
	}

	// --- Vehicle serialization through the generic operations ------------------

	@Test
	void vehicleListGetDeserializesNestedFieldsAndStatusEnum() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(200,
				"[{\"id\":1,\"registrationNumber\":\"A\",\"brand\":\"B\",\"model\":\"M\","
						+ "\"dailyPrice\":9.99,\"status\":\"MAINTENANCE\",\"categoryId\":2,"
						+ "\"categoryName\":\"Van\"}]"));

		List<be.condorcet.easycarrent.desktop.dto.VehicleResponseDto> vehicles = client(http)
				.getJsonList("/api/vehicles",
						be.condorcet.easycarrent.desktop.dto.VehicleResponseDto.class, testCredentials())
				.join();

		assertEquals("http://localhost:8080/api/vehicles", http.lastRequest().uri().toString());
		assertEquals(1, vehicles.size());
		assertEquals(be.condorcet.easycarrent.desktop.dto.VehicleStatus.MAINTENANCE,
				vehicles.get(0).status());
		assertEquals(new java.math.BigDecimal("9.99"), vehicles.get(0).dailyPrice());
		assertEquals("Van", vehicles.get(0).categoryName());
	}

	@Test
	void vehicleCreateSerializesBigDecimalAndOmitsStatus() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201,
				"{\"id\":9,\"registrationNumber\":\"A\",\"brand\":\"B\",\"model\":\"M\","
						+ "\"dailyPrice\":42.50,\"status\":\"AVAILABLE\",\"categoryId\":3,"
						+ "\"categoryName\":\"C\"}"));
		var request = new be.condorcet.easycarrent.desktop.dto.VehicleRequestDto(
				"A", "B", "M", 2022, "Blue", new java.math.BigDecimal("42.50"), 100L, 3L);

		client(http).postJson("/api/vehicles", request,
				be.condorcet.easycarrent.desktop.dto.VehicleResponseDto.class, testCredentials()).join();

		assertEquals("POST", http.lastRequest().method());
		assertTrue(http.lastRequestBody().contains("\"dailyPrice\":42.50"));
		assertTrue(http.lastRequestBody().contains("\"categoryId\":3"));
		assertFalse(http.lastRequestBody().contains("\"status\""));
	}

	@Test
	void customerCreateRoundTripsLocalDate() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(201,
				"{\"id\":9,\"firstName\":\"Test\",\"lastName\":\"Customer\","
						+ "\"email\":\"test.customer@example.invalid\",\"phone\":\"+0000000000\","
						+ "\"address\":\"x\",\"drivingLicenseNumber\":\"L\","
						+ "\"drivingLicenseExpiryDate\":\"2030-01-15\"}"));
		var request = new be.condorcet.easycarrent.desktop.dto.CustomerRequestDto(
				"Test", "Customer", "test.customer@example.invalid", "+0000000000", "x", "L",
				java.time.LocalDate.of(2030, 1, 15));

		var created = client(http).postJson("/api/customers", request,
				be.condorcet.easycarrent.desktop.dto.CustomerResponseDto.class, testCredentials()).join();

		assertTrue(http.lastRequestBody().contains("\"drivingLicenseExpiryDate\":\"2030-01-15\""));
		assertEquals(java.time.LocalDate.of(2030, 1, 15), created.drivingLicenseExpiryDate());
	}
}

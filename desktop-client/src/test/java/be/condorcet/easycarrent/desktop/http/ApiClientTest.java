package be.condorcet.easycarrent.desktop.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class ApiClientTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");
	private static final Duration TIMEOUT = Duration.ofSeconds(7);
	private static final String PING_PATH = "/api/ping";
	private static final String PING_BODY = "Easy Car Rent API is running";

	private ApiClient client(RecordingHttpClient http) {
		return new ApiClient(BASE_URI, http, JsonMapperFactory.create(), TIMEOUT);
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
}

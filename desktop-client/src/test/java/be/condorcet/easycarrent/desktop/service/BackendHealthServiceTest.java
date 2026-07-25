package be.condorcet.easycarrent.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.JsonMapperFactory;
import be.condorcet.easycarrent.desktop.support.FakeHttpResponse;
import be.condorcet.easycarrent.desktop.support.RecordingHttpClient;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class BackendHealthServiceTest {

	private static final URI BASE_URI = URI.create("http://localhost:8080/");

	private BackendHealthService serviceBackedBy(RecordingHttpClient http) {
		ApiClient apiClient =
				new ApiClient(BASE_URI, http, JsonMapperFactory.create(), Duration.ofSeconds(10));
		return new BackendHealthService(apiClient);
	}

	@Test
	void successfulPingProducesConnectedResult() {
		RecordingHttpClient http = RecordingHttpClient
				.returning(new FakeHttpResponse(200, "Easy Car Rent API is running"));

		BackendHealthResult result = serviceBackedBy(http).checkHealth().join();

		assertEquals(BackendHealthResult.Status.CONNECTED, result.status());
		assertEquals("Backend connected", result.message());
	}

	@Test
	void transportFailureProducesUnavailableResult() {
		RecordingHttpClient http =
				RecordingHttpClient.failingWith(new ConnectException("Connection refused"));

		BackendHealthResult result = serviceBackedBy(http).checkHealth().join();

		assertEquals(BackendHealthResult.Status.UNAVAILABLE, result.status());
		assertFalse(result.isConnected());
	}

	@Test
	void nonSuccessResponseProducesUnexpectedResult() {
		RecordingHttpClient http = RecordingHttpClient.returning(new FakeHttpResponse(500, "boom"));

		BackendHealthResult result = serviceBackedBy(http).checkHealth().join();

		assertEquals(BackendHealthResult.Status.UNEXPECTED, result.status());
		assertFalse(result.isConnected());
	}
}

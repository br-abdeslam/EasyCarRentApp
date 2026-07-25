package be.condorcet.easycarrent.desktop.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLSession;

/**
 * Minimal {@link HttpResponse} of {@code String} for tests. Only the status code
 * and body are relevant to {@code ApiClient}; the remaining accessors return
 * empty/default values.
 */
public record FakeHttpResponse(int statusCode, String body) implements HttpResponse<String> {

	@Override
	public HttpRequest request() {
		return null;
	}

	@Override
	public Optional<HttpResponse<String>> previousResponse() {
		return Optional.empty();
	}

	@Override
	public HttpHeaders headers() {
		return HttpHeaders.of(Map.of(), (name, value) -> true);
	}

	@Override
	public Optional<SSLSession> sslSession() {
		return Optional.empty();
	}

	@Override
	public URI uri() {
		return null;
	}

	@Override
	public HttpClient.Version version() {
		return HttpClient.Version.HTTP_1_1;
	}
}

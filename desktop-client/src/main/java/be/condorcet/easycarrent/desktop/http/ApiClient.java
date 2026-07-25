package be.condorcet.easycarrent.desktop.http;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Reusable, non-blocking HTTP client for the Easy Car Rent backend REST API.
 *
 * <p>Wraps a single {@link HttpClient} instance and the shared JSON
 * {@link ObjectMapper}. All operations are asynchronous and return a
 * {@link CompletableFuture}; nothing here blocks the calling thread. Transport
 * failures surface as {@link ApiConnectionException} and non-2xx responses as
 * {@link ApiRequestException}. This class contains no UI logic.</p>
 *
 * <p>Requests are anonymous by default. HTTP Basic authentication is applied
 * only when {@link BasicCredentials} are supplied explicitly to an authenticated
 * operation; the generated {@code Authorization} header is never retained,
 * logged, or included in exceptions.</p>
 */
public final class ApiClient {

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private static final String ACCEPT_JSON = "application/json";
	private static final String ACCEPT_TEXT = "text/plain, application/json";

	private final URI baseUri;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final Duration requestTimeout;

	/**
	 * Creates a client with sensible defaults for the given base URI.
	 *
	 * @param baseUri normalized backend base URI (expected to end with a slash)
	 */
	public ApiClient(URI baseUri) {
		this(baseUri, defaultHttpClient(), JsonMapperFactory.create(), DEFAULT_REQUEST_TIMEOUT);
	}

	/**
	 * Creates a client with explicit collaborators. Primarily used by tests to
	 * inject a custom {@link HttpClient} or request timeout.
	 */
	public ApiClient(URI baseUri, HttpClient httpClient, ObjectMapper objectMapper,
			Duration requestTimeout) {
		this.baseUri = ensureTrailingSlash(Objects.requireNonNull(baseUri, "baseUri"));
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
	}

	private static HttpClient defaultHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(DEFAULT_CONNECT_TIMEOUT)
				.build();
	}

	/**
	 * Performs an asynchronous {@code GET} expecting a plain-text (or text-like)
	 * body, such as {@code /api/ping}.
	 *
	 * @param path API path, absolute ({@code /api/ping}) or relative
	 * @return a future completing with the response body, or empty for 204
	 */
	public CompletableFuture<String> getText(String path) {
		HttpRequest request = buildGet(path, ACCEPT_TEXT, null);
		return send(request).thenApply(response -> readTextBody(path, response));
	}

	/**
	 * Performs an asynchronous authenticated {@code GET} expecting a text or JSON
	 * body, applying HTTP Basic authentication from the supplied credentials.
	 *
	 * @param path        API path, absolute or relative
	 * @param credentials credentials used to build the {@code Authorization} header
	 * @return a future completing with the response body, or empty for 204
	 */
	public CompletableFuture<String> getText(String path, BasicCredentials credentials) {
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = buildGet(path, ACCEPT_TEXT, credentials);
		return send(request).thenApply(response -> readTextBody(path, response));
	}

	/**
	 * Performs an asynchronous {@code GET} expecting a JSON body deserialized to
	 * the given type.
	 *
	 * @param path API path, absolute or relative
	 * @param type target type for the JSON body
	 * @return a future completing with the deserialized value, or null for 204
	 */
	public <T> CompletableFuture<T> getJson(String path, Class<T> type) {
		Objects.requireNonNull(type, "type");
		HttpRequest request = buildGet(path, ACCEPT_JSON, null);
		return send(request).thenApply(response -> readJsonBody(path, response, type));
	}

	/**
	 * Performs an asynchronous authenticated {@code GET} deserializing a JSON body
	 * to the given type.
	 */
	public <T> CompletableFuture<T> getJson(String path, Class<T> type, BasicCredentials credentials) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = jsonRequest("GET", path, null, credentials);
		return send(request).thenApply(response -> readJsonBody(path, response, type));
	}

	/**
	 * Performs an asynchronous authenticated {@code GET} deserializing a JSON array
	 * to a list of the given element type. A 204 or empty body yields an empty list.
	 */
	public <T> CompletableFuture<List<T>> getJsonList(String path, Class<T> elementType,
			BasicCredentials credentials) {
		Objects.requireNonNull(elementType, "elementType");
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = jsonRequest("GET", path, null, credentials);
		return send(request).thenApply(response -> readJsonListBody(path, response, elementType));
	}

	/**
	 * Performs an asynchronous authenticated {@code POST} with a JSON request body
	 * and a typed JSON response.
	 */
	public <T> CompletableFuture<T> postJson(String path, Object body, Class<T> responseType,
			BasicCredentials credentials) {
		Objects.requireNonNull(body, "body");
		Objects.requireNonNull(responseType, "responseType");
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = jsonRequest("POST", path, serialize(body), credentials);
		return send(request).thenApply(response -> readJsonBody(path, response, responseType));
	}

	/**
	 * Performs an asynchronous authenticated {@code PUT} with a JSON request body
	 * and a typed JSON response.
	 */
	public <T> CompletableFuture<T> putJson(String path, Object body, Class<T> responseType,
			BasicCredentials credentials) {
		Objects.requireNonNull(body, "body");
		Objects.requireNonNull(responseType, "responseType");
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = jsonRequest("PUT", path, serialize(body), credentials);
		return send(request).thenApply(response -> readJsonBody(path, response, responseType));
	}

	/**
	 * Performs an asynchronous authenticated {@code DELETE}. Any 2xx (including 204)
	 * completes successfully; non-2xx becomes an {@link ApiRequestException}.
	 */
	public CompletableFuture<Void> delete(String path, BasicCredentials credentials) {
		Objects.requireNonNull(credentials, "credentials");
		HttpRequest request = jsonRequest("DELETE", path, null, credentials);
		return send(request).thenApply(response -> {
			ensureSuccess(path, response);
			return null;
		});
	}

	private HttpRequest buildGet(String path, String acceptValue, BasicCredentials credentials) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
				.timeout(requestTimeout)
				.header("Accept", acceptValue);
		if (credentials != null) {
			builder.header("Authorization", credentials.toAuthorizationHeader());
		}
		return builder.GET().build();
	}

	private HttpRequest jsonRequest(String method, String path, String bodyJson,
			BasicCredentials credentials) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
				.timeout(requestTimeout)
				.header("Accept", ACCEPT_JSON);
		if (credentials != null) {
			builder.header("Authorization", credentials.toAuthorizationHeader());
		}
		if (bodyJson != null) {
			builder.header("Content-Type", ACCEPT_JSON);
			builder.method(method, HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8));
		} else {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}
		return builder.build();
	}

	private String serialize(Object body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to serialize the request body", e);
		}
	}

	private CompletableFuture<HttpResponse<String>> send(HttpRequest request) {
		return httpClient
				.sendAsync(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8))
				.handle((response, throwable) -> {
					if (throwable != null) {
						throw new ApiConnectionException(
								"Unable to reach the backend", unwrap(throwable));
					}
					return response;
				});
	}

	private String readTextBody(String path, HttpResponse<String> response) {
		int status = response.statusCode();
		if (isSuccess(status)) {
			return status == 204 ? "" : response.body();
		}
		throw toRequestException(path, response);
	}

	private <T> T readJsonBody(String path, HttpResponse<String> response, Class<T> type) {
		int status = response.statusCode();
		if (isSuccess(status)) {
			String body = response.body();
			if (status == 204 || body == null || body.isBlank()) {
				return null;
			}
			try {
				return objectMapper.readValue(body, type);
			} catch (IOException e) {
				throw new ApiConnectionException(
						"The backend returned an unreadable response body", e);
			}
		}
		throw toRequestException(path, response);
	}

	private <T> List<T> readJsonListBody(String path, HttpResponse<String> response,
			Class<T> elementType) {
		int status = response.statusCode();
		if (isSuccess(status)) {
			String body = response.body();
			if (status == 204 || body == null || body.isBlank()) {
				return List.of();
			}
			try {
				JavaType listType = objectMapper.getTypeFactory()
						.constructCollectionType(List.class, elementType);
				return objectMapper.readValue(body, listType);
			} catch (IOException e) {
				throw new ApiConnectionException(
						"The backend returned an unreadable response body", e);
			}
		}
		throw toRequestException(path, response);
	}

	private void ensureSuccess(String path, HttpResponse<String> response) {
		if (!isSuccess(response.statusCode())) {
			throw toRequestException(path, response);
		}
	}

	private ApiRequestException toRequestException(String path, HttpResponse<String> response) {
		int status = response.statusCode();
		ApiErrorDto apiError = parseError(response.body());
		String message = (apiError != null && apiError.message() != null
				&& !apiError.message().isBlank())
						? apiError.message()
						: "The backend request failed with HTTP status " + status;
		return new ApiRequestException(status, message, apiError, path);
	}

	private ApiErrorDto parseError(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(body, ApiErrorDto.class);
		} catch (IOException e) {
			return null;
		}
	}

	private URI resolve(String path) {
		Objects.requireNonNull(path, "path");
		String relative = path.startsWith("/") ? path.substring(1) : path;
		return baseUri.resolve(relative);
	}

	private static URI ensureTrailingSlash(URI uri) {
		String text = uri.toString();
		return text.endsWith("/") ? uri : URI.create(text + "/");
	}

	private static boolean isSuccess(int status) {
		return status >= 200 && status < 300;
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}

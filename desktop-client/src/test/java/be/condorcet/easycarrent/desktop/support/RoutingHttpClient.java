package be.condorcet.easycarrent.desktop.support;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Test {@link HttpClient} that returns a different response per request path, so a
 * component making several concurrent requests to different endpoints (such as the
 * dashboard load) can be exercised deterministically without a network. Each path
 * may be configured to return a status/body or to fail at the transport level; an
 * unconfigured path returns an empty JSON list. It records how many requests were
 * made so a test can assert that every source endpoint was invoked.
 */
public final class RoutingHttpClient extends HttpClient {

	private final Map<String, Supplier<CompletableFuture<HttpResponse<String>>>> routes =
			new LinkedHashMap<>();
	private final AtomicInteger callCount = new AtomicInteger();

	/** Configures {@code path} to return {@code body} with HTTP 200. */
	public RoutingHttpClient ok(String path, String body) {
		routes.put(path, () -> CompletableFuture.completedFuture(new FakeHttpResponse(200, body)));
		return this;
	}

	/** Configures {@code path} to return the given status and body. */
	public RoutingHttpClient status(String path, int statusCode, String body) {
		routes.put(path, () -> CompletableFuture.completedFuture(new FakeHttpResponse(statusCode, body)));
		return this;
	}

	/** Configures {@code path} to fail at the transport level with {@code cause}. */
	public RoutingHttpClient fail(String path, Throwable cause) {
		routes.put(path, () -> CompletableFuture.failedFuture(cause));
		return this;
	}

	/** @return the number of requests sent so far. */
	public int callCount() {
		return callCount.get();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			BodyHandler<T> responseBodyHandler) {
		callCount.incrementAndGet();
		String path = request.uri().getPath();
		Supplier<CompletableFuture<HttpResponse<String>>> route = routes.get(path);
		CompletableFuture<HttpResponse<String>> response = route != null
				? route.get()
				: CompletableFuture.completedFuture(new FakeHttpResponse(200, "[]"));
		return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) response;
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
		return sendAsync(request, responseBodyHandler);
	}

	@Override
	public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
		throw new UnsupportedOperationException("synchronous send is not used");
	}

	@Override
	public Optional<CookieHandler> cookieHandler() {
		return Optional.empty();
	}

	@Override
	public Optional<Duration> connectTimeout() {
		return Optional.empty();
	}

	@Override
	public Redirect followRedirects() {
		return Redirect.NEVER;
	}

	@Override
	public Optional<ProxySelector> proxy() {
		return Optional.empty();
	}

	@Override
	public SSLContext sslContext() {
		return null;
	}

	@Override
	public SSLParameters sslParameters() {
		return new SSLParameters();
	}

	@Override
	public Optional<Authenticator> authenticator() {
		return Optional.empty();
	}

	@Override
	public Version version() {
		return Version.HTTP_1_1;
	}

	@Override
	public Optional<Executor> executor() {
		return Optional.empty();
	}
}

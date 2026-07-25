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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Test {@link HttpClient} that records the last request it was asked to send and
 * returns a pre-configured response future. Lets {@code ApiClient} be exercised
 * deterministically without a network, a port, or a mocking framework.
 */
public final class RecordingHttpClient extends HttpClient {

	private final CompletableFuture<HttpResponse<String>> responseFuture;
	private volatile HttpRequest lastRequest;

	private RecordingHttpClient(CompletableFuture<HttpResponse<String>> responseFuture) {
		this.responseFuture = responseFuture;
	}

	/** A client that returns the given response for the next request. */
	public static RecordingHttpClient returning(HttpResponse<String> response) {
		return new RecordingHttpClient(CompletableFuture.completedFuture(response));
	}

	/** A client whose request fails at the transport level with {@code cause}. */
	public static RecordingHttpClient failingWith(Throwable cause) {
		return new RecordingHttpClient(CompletableFuture.failedFuture(cause));
	}

	/** A client backed by a future the test completes later, to prove non-blocking. */
	public static RecordingHttpClient deferred(CompletableFuture<HttpResponse<String>> future) {
		return new RecordingHttpClient(future);
	}

	public HttpRequest lastRequest() {
		return lastRequest;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			BodyHandler<T> responseBodyHandler) {
		this.lastRequest = request;
		return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) responseFuture;
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

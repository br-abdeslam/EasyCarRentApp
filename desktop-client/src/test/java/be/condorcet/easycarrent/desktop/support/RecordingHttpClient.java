package be.condorcet.easycarrent.desktop.support;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

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
	private volatile String lastRequestBody;

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

	/** @return the UTF-8 body of the last request, or null if it had no body */
	public String lastRequestBody() {
		return lastRequestBody;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			BodyHandler<T> responseBodyHandler) {
		this.lastRequest = request;
		this.lastRequestBody = extractBody(request);
		return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) responseFuture;
	}

	private static String extractBody(HttpRequest request) {
		return request.bodyPublisher()
				.map(RecordingHttpClient::readPublisher)
				.orElse(null);
	}

	private static String readPublisher(HttpRequest.BodyPublisher publisher) {
		StringBuilder body = new StringBuilder();
		CountDownLatch done = new CountDownLatch(1);
		publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ByteBuffer item) {
				body.append(StandardCharsets.UTF_8.decode(item));
			}

			@Override
			public void onError(Throwable throwable) {
				done.countDown();
			}

			@Override
			public void onComplete() {
				done.countDown();
			}
		});
		try {
			done.await(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return body.toString();
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

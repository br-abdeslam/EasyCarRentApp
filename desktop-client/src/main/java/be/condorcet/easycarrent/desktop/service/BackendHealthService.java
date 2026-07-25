package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Connectivity use case: checks whether the backend is reachable by calling the
 * public {@code /api/ping} endpoint.
 *
 * <p>Depends only on {@link ApiClient}, returns a {@link CompletableFuture}, and
 * never touches JavaFX. It maps transport failures and unexpected responses onto
 * a small, display-safe {@link BackendHealthResult}.</p>
 */
public final class BackendHealthService {

	static final String PING_PATH = "/api/ping";

	private final ApiClient apiClient;

	public BackendHealthService(ApiClient apiClient) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
	}

	/**
	 * Checks backend connectivity without blocking the caller.
	 *
	 * @return a future completing with the connectivity result (never failing)
	 */
	public CompletableFuture<BackendHealthResult> checkHealth() {
		return apiClient.getText(PING_PATH)
				.handle((body, throwable) -> {
					if (throwable != null) {
						return mapFailure(throwable);
					}
					return BackendHealthResult.connected("Backend connected");
				});
	}

	private BackendHealthResult mapFailure(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiRequestException) {
			return BackendHealthResult.unexpected("Backend responded unexpectedly");
		}
		if (cause instanceof ApiConnectionException) {
			return BackendHealthResult.unavailable("Backend unavailable");
		}
		return BackendHealthResult.unavailable("Backend unavailable");
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}

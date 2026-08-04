package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.RentalRequestDto;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Rental API workflow.
 *
 * <p>Maps the desktop's rental operations onto the backend endpoints under
 * {@code /api/rentals}, using the current authenticated credentials from the
 * shared {@link SessionManager} and the generic {@link ApiClient} transport. The
 * lifecycle transitions (start, complete, cancel) call the backend's dedicated
 * {@code PATCH} endpoints so the backend's transition rules are never bypassed by a
 * generic update. It returns {@link CompletableFuture} values, preserves the
 * backend list ordering, and propagates {@code ApiRequestException} and
 * {@code ApiConnectionException} unchanged. It holds no JavaFX state, no Stage, no
 * dialogs, and no password beyond the session it reads.</p>
 */
public final class RentalService {

	static final String RENTALS_PATH = "/api/rentals";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public RentalService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all rentals in the backend's order. */
	public CompletableFuture<List<RentalResponseDto>> findAll() {
		return apiClient.getJsonList(RENTALS_PATH, RentalResponseDto.class, requireCredentials());
	}

	/** Creates a rental (booking). The backend sets the status to PLANNED. */
	public CompletableFuture<RentalResponseDto> create(RentalRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(RENTALS_PATH, request, RentalResponseDto.class, requireCredentials());
	}

	/** Updates the PLANNED rental with the given id. */
	public CompletableFuture<RentalResponseDto> update(long id, RentalRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.putJson(resourcePath(id), request, RentalResponseDto.class, requireCredentials());
	}

	/** Deletes the rental with the given id (ADMIN only on the backend). */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	/** Starts the PLANNED rental with the given id (PLANNED &rarr; ACTIVE). */
	public CompletableFuture<RentalResponseDto> start(long id) {
		return apiClient.patchJson(transitionPath(id, "start"), RentalResponseDto.class,
				requireCredentials());
	}

	/** Completes the ACTIVE rental with the given id (ACTIVE &rarr; COMPLETED). */
	public CompletableFuture<RentalResponseDto> complete(long id) {
		return apiClient.patchJson(transitionPath(id, "complete"), RentalResponseDto.class,
				requireCredentials());
	}

	/** Cancels the PLANNED rental with the given id (PLANNED &rarr; CANCELLED). */
	public CompletableFuture<RentalResponseDto> cancel(long id) {
		return apiClient.patchJson(transitionPath(id, "cancel"), RentalResponseDto.class,
				requireCredentials());
	}

	private static String resourcePath(long id) {
		return RENTALS_PATH + "/" + requirePositive(id);
	}

	private static String transitionPath(long id, String action) {
		return RENTALS_PATH + "/" + requirePositive(id) + "/" + action;
	}

	private static long requirePositive(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("rental id must be positive");
		}
		return id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

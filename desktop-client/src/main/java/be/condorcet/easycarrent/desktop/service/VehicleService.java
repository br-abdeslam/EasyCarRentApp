package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.VehicleRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Vehicle API workflow.
 *
 * <p>Maps the desktop's vehicle operations onto the backend endpoints under
 * {@code /api/vehicles}, using the current authenticated credentials from the
 * shared {@link SessionManager} and the generic {@link ApiClient} transport. It
 * returns {@link CompletableFuture} values, preserves the backend list ordering,
 * and propagates {@code ApiRequestException} and {@code ApiConnectionException}
 * unchanged. It holds no JavaFX state, no Stage, no dialogs, and no password
 * beyond the session it reads.</p>
 */
public final class VehicleService {

	static final String VEHICLES_PATH = "/api/vehicles";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public VehicleService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all vehicles in the backend's order. */
	public CompletableFuture<List<VehicleResponseDto>> findAll() {
		return apiClient.getJsonList(VEHICLES_PATH, VehicleResponseDto.class, requireCredentials());
	}

	/** Creates a vehicle. */
	public CompletableFuture<VehicleResponseDto> create(VehicleRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(VEHICLES_PATH, request, VehicleResponseDto.class,
				requireCredentials());
	}

	/** Updates the vehicle with the given id. */
	public CompletableFuture<VehicleResponseDto> update(long id, VehicleRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.putJson(resourcePath(id), request, VehicleResponseDto.class,
				requireCredentials());
	}

	/** Deletes the vehicle with the given id. */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	private static String resourcePath(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("vehicle id must be positive");
		}
		return VEHICLES_PATH + "/" + id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.MaintenanceRequestDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Maintenance API workflow.
 *
 * <p>Maps the desktop's maintenance operations onto the backend endpoints under
 * {@code /api/maintenance-records}, using the current authenticated credentials from
 * the shared {@link SessionManager} and the generic {@link ApiClient} transport. The
 * lifecycle transitions (start, complete) call the backend's dedicated body-less
 * {@code PATCH} endpoints so the backend's transition rules and vehicle-status
 * synchronization are never bypassed; there is no maintenance update operation on
 * the backend. It returns {@link CompletableFuture} values, preserves the backend
 * list ordering, and propagates {@code ApiRequestException} and
 * {@code ApiConnectionException} unchanged. It holds no JavaFX state, no Stage, no
 * dialogs, and no password beyond the session it reads.</p>
 */
public final class MaintenanceService {

	static final String MAINTENANCE_PATH = "/api/maintenance-records";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public MaintenanceService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all maintenance records in the backend's order. */
	public CompletableFuture<List<MaintenanceResponseDto>> findAll() {
		return apiClient.getJsonList(MAINTENANCE_PATH, MaintenanceResponseDto.class,
				requireCredentials());
	}

	/** Creates a maintenance record (ADMIN only). The backend sets the status to PLANNED. */
	public CompletableFuture<MaintenanceResponseDto> create(MaintenanceRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(MAINTENANCE_PATH, request, MaintenanceResponseDto.class,
				requireCredentials());
	}

	/** Deletes the maintenance record with the given id (ADMIN only; only PLANNED is deletable). */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	/** Starts the PLANNED record with the given id (PLANNED &rarr; IN_PROGRESS; ADMIN only). */
	public CompletableFuture<MaintenanceResponseDto> start(long id) {
		return apiClient.patchJson(transitionPath(id, "start"), MaintenanceResponseDto.class,
				requireCredentials());
	}

	/** Completes the IN_PROGRESS record with the given id (IN_PROGRESS &rarr; COMPLETED; ADMIN only). */
	public CompletableFuture<MaintenanceResponseDto> complete(long id) {
		return apiClient.patchJson(transitionPath(id, "complete"), MaintenanceResponseDto.class,
				requireCredentials());
	}

	private static String resourcePath(long id) {
		return MAINTENANCE_PATH + "/" + requirePositive(id);
	}

	private static String transitionPath(long id, String action) {
		return MAINTENANCE_PATH + "/" + requirePositive(id) + "/" + action;
	}

	private static long requirePositive(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("maintenance record id must be positive");
		}
		return id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Vehicle-category API workflow.
 *
 * <p>Maps the desktop's category operations onto the backend endpoints under
 * {@code /api/categories}, using the current authenticated credentials from the
 * shared {@link SessionManager}. It returns {@link CompletableFuture} values,
 * preserves the backend list ordering, and propagates {@code ApiRequestException}
 * and {@code ApiConnectionException} unchanged. It holds no JavaFX state, no
 * Stage, no dialogs, and no password beyond the session it reads.</p>
 */
public final class VehicleCategoryService {

	static final String CATEGORIES_PATH = "/api/categories";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public VehicleCategoryService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all categories in the backend's order. */
	public CompletableFuture<List<VehicleCategoryResponseDto>> findAll() {
		return apiClient.getJsonList(CATEGORIES_PATH, VehicleCategoryResponseDto.class,
				requireCredentials());
	}

	/** Creates a category. */
	public CompletableFuture<VehicleCategoryResponseDto> create(VehicleCategoryRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(CATEGORIES_PATH, request, VehicleCategoryResponseDto.class,
				requireCredentials());
	}

	/** Updates the category with the given id. */
	public CompletableFuture<VehicleCategoryResponseDto> update(long id,
			VehicleCategoryRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.putJson(resourcePath(id), request, VehicleCategoryResponseDto.class,
				requireCredentials());
	}

	/** Deletes the category with the given id. */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	private static String resourcePath(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("category id must be positive");
		}
		return CATEGORIES_PATH + "/" + id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

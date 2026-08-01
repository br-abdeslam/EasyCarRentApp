package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;
import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Customer API workflow.
 *
 * <p>Maps the desktop's customer operations onto the backend endpoints under
 * {@code /api/customers}, using the current authenticated credentials from the
 * shared {@link SessionManager} and the generic {@link ApiClient} transport. It
 * returns {@link CompletableFuture} values, preserves the backend list ordering,
 * and propagates {@code ApiRequestException} and {@code ApiConnectionException}
 * unchanged. It holds no JavaFX state, no Stage, no dialogs, and no password
 * beyond the session it reads, and it never logs customer data.</p>
 */
public final class CustomerService {

	static final String CUSTOMERS_PATH = "/api/customers";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public CustomerService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all customers in the backend's order. */
	public CompletableFuture<List<CustomerResponseDto>> findAll() {
		return apiClient.getJsonList(CUSTOMERS_PATH, CustomerResponseDto.class, requireCredentials());
	}

	/** Creates a customer. */
	public CompletableFuture<CustomerResponseDto> create(CustomerRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(CUSTOMERS_PATH, request, CustomerResponseDto.class,
				requireCredentials());
	}

	/** Updates the customer with the given id. */
	public CompletableFuture<CustomerResponseDto> update(long id, CustomerRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.putJson(resourcePath(id), request, CustomerResponseDto.class,
				requireCredentials());
	}

	/** Deletes the customer with the given id. */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	private static String resourcePath(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("customer id must be positive");
		}
		return CUSTOMERS_PATH + "/" + id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

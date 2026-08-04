package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.dto.PaymentRequestDto;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Payment API workflow.
 *
 * <p>Maps the desktop's payment operations onto the backend endpoints under
 * {@code /api/payments}, using the current authenticated credentials from the
 * shared {@link SessionManager} and the generic {@link ApiClient} transport. The
 * lifecycle transitions (pay, fail, retry, refund) call the backend's dedicated
 * body-less {@code PATCH} endpoints so the backend's transition rules are never
 * bypassed; there is no payment update operation on the backend. It returns
 * {@link CompletableFuture} values, preserves the backend list ordering, and
 * propagates {@code ApiRequestException} and {@code ApiConnectionException}
 * unchanged. It holds no JavaFX state, no Stage, no dialogs, and no password beyond
 * the session it reads.</p>
 */
public final class PaymentService {

	static final String PAYMENTS_PATH = "/api/payments";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public PaymentService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/** Lists all payments in the backend's order. */
	public CompletableFuture<List<PaymentResponseDto>> findAll() {
		return apiClient.getJsonList(PAYMENTS_PATH, PaymentResponseDto.class, requireCredentials());
	}

	/** Returns the single payment for a rental (404 when the rental has none). */
	public CompletableFuture<PaymentResponseDto> findByRental(long rentalId) {
		String path = PAYMENTS_PATH + "/rental/" + requirePositive(rentalId);
		return apiClient.getJson(path, PaymentResponseDto.class, requireCredentials());
	}

	/** Creates a payment; the backend derives the amount and sets the status to PENDING. */
	public CompletableFuture<PaymentResponseDto> create(PaymentRequestDto request) {
		Objects.requireNonNull(request, "request");
		return apiClient.postJson(PAYMENTS_PATH, request, PaymentResponseDto.class, requireCredentials());
	}

	/** Deletes the payment with the given id (ADMIN only; PAID/REFUNDED are not deletable). */
	public CompletableFuture<Void> delete(long id) {
		return apiClient.delete(resourcePath(id), requireCredentials());
	}

	/** Marks the PENDING payment paid (PENDING &rarr; PAID). */
	public CompletableFuture<PaymentResponseDto> markPaid(long id) {
		return apiClient.patchJson(transitionPath(id, "pay"), PaymentResponseDto.class,
				requireCredentials());
	}

	/** Marks the PENDING payment failed (PENDING &rarr; FAILED). */
	public CompletableFuture<PaymentResponseDto> markFailed(long id) {
		return apiClient.patchJson(transitionPath(id, "fail"), PaymentResponseDto.class,
				requireCredentials());
	}

	/** Retries the FAILED payment (FAILED &rarr; PENDING). */
	public CompletableFuture<PaymentResponseDto> retry(long id) {
		return apiClient.patchJson(transitionPath(id, "retry"), PaymentResponseDto.class,
				requireCredentials());
	}

	/** Refunds the PAID payment (PAID &rarr; REFUNDED; ADMIN only on the backend). */
	public CompletableFuture<PaymentResponseDto> refund(long id) {
		return apiClient.patchJson(transitionPath(id, "refund"), PaymentResponseDto.class,
				requireCredentials());
	}

	private static String resourcePath(long id) {
		return PAYMENTS_PATH + "/" + requirePositive(id);
	}

	private static String transitionPath(long id, String action) {
		return PAYMENTS_PATH + "/" + requirePositive(id) + "/" + action;
	}

	private static long requirePositive(long id) {
		if (id <= 0) {
			throw new IllegalArgumentException("payment id must be positive");
		}
		return id;
	}

	private BasicCredentials requireCredentials() {
		return sessionManager.currentCredentials()
				.orElseThrow(() -> new IllegalStateException("No authenticated session is present"));
	}
}

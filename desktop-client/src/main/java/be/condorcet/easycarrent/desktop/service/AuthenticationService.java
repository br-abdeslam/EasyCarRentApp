package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Verifies credentials against the backend and establishes the in-memory session.
 *
 * <p>The backend exposes no authenticated identity endpoint, so credentials are
 * validated with an authenticated GET against a harmless protected read
 * ({@code /api/vehicles}). Only after a successful (2xx) response is the role
 * resolved from the fixed development accounts and the session started. Invalid
 * credentials and transport failures never create a session. This class holds no
 * JavaFX state and never puts credentials in result messages.</p>
 */
public final class AuthenticationService {

	/** Harmless protected read used purely to validate credentials. */
	static final String VALIDATION_PATH = "/api/vehicles";

	private final ApiClient apiClient;
	private final SessionManager sessionManager;

	public AuthenticationService(ApiClient apiClient, SessionManager sessionManager) {
		this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
		this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
	}

	/**
	 * Authenticates the given credentials without blocking the caller.
	 *
	 * @param credentials the credentials to verify
	 * @return a future completing with the outcome (never completing exceptionally)
	 */
	public CompletableFuture<AuthenticationResult> authenticate(BasicCredentials credentials) {
		Objects.requireNonNull(credentials, "credentials");
		return apiClient.getText(VALIDATION_PATH, credentials)
				.handle((body, throwable) -> {
					if (throwable != null) {
						return mapFailure(throwable);
					}
					return onValidated(credentials);
				});
	}

	private AuthenticationResult onValidated(BasicCredentials credentials) {
		Optional<DesktopUserRole> role =
				DesktopUserRole.forDevelopmentUsername(credentials.username());
		if (role.isEmpty()) {
			return AuthenticationResult.unexpectedError(
					"Authenticated user is not recognized by this client");
		}
		sessionManager.start(new AuthenticatedUser(credentials.username(), role.get()), credentials);
		return AuthenticationResult.authenticated("Signed in as " + credentials.username());
	}

	private AuthenticationResult mapFailure(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiRequestException requestException) {
			return mapRequestException(requestException);
		}
		if (cause instanceof ApiConnectionException) {
			return AuthenticationResult.backendUnavailable(
					"The backend is unavailable. Please try again.");
		}
		return AuthenticationResult.unexpectedError("An unexpected error occurred.");
	}

	private AuthenticationResult mapRequestException(ApiRequestException exception) {
		int status = exception.status();
		if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
			return AuthenticationResult.invalidCredentials("Invalid username or password.");
		}
		if (status == HttpURLConnection.HTTP_FORBIDDEN) {
			return AuthenticationResult.unexpectedError(
					"This account is not authorized for the desktop client.");
		}
		return AuthenticationResult.unexpectedError(
				"Unexpected response from the backend (status " + status + ").");
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}

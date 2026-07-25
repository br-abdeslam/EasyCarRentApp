package be.condorcet.easycarrent.desktop.service;

/**
 * Immutable, display-safe outcome of a login attempt.
 *
 * <p>The {@link #message()} is always safe to show to a user and never contains
 * credentials.</p>
 *
 * @param status  the authentication outcome
 * @param message a short message safe to display
 */
public record AuthenticationResult(Status status, String message) {

	/** Authentication outcomes distinguished by the service. */
	public enum Status {
		/** Credentials were accepted by the backend. */
		AUTHENTICATED,
		/** The backend rejected the credentials (HTTP 401). */
		INVALID_CREDENTIALS,
		/** No response could be obtained from the backend. */
		BACKEND_UNAVAILABLE,
		/** Any other unexpected response or error. */
		UNEXPECTED_ERROR
	}

	public static AuthenticationResult authenticated(String message) {
		return new AuthenticationResult(Status.AUTHENTICATED, message);
	}

	public static AuthenticationResult invalidCredentials(String message) {
		return new AuthenticationResult(Status.INVALID_CREDENTIALS, message);
	}

	public static AuthenticationResult backendUnavailable(String message) {
		return new AuthenticationResult(Status.BACKEND_UNAVAILABLE, message);
	}

	public static AuthenticationResult unexpectedError(String message) {
		return new AuthenticationResult(Status.UNEXPECTED_ERROR, message);
	}

	public boolean isAuthenticated() {
		return status == Status.AUTHENTICATED;
	}
}

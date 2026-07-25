package be.condorcet.easycarrent.desktop.session;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;

import java.util.Objects;
import java.util.Optional;

/**
 * Application-memory holder for the current authenticated session.
 *
 * <p>Holds at most one {@link UserSession} in a private mutable field. It is
 * created once during application bootstrap and injected where needed; it uses
 * no files, preferences, or database, and keeps no static mutable state. A fresh
 * instance is trivially created in tests. {@link #logout()} drops every
 * reference, so old credentials become unreachable after logout.</p>
 */
public final class SessionManager {

	private UserSession session;

	/** Establishes a new authenticated session, replacing any previous one. */
	public void start(AuthenticatedUser user, BasicCredentials credentials) {
		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(credentials, "credentials must not be null");
		this.session = new UserSession(user, credentials);
	}

	public boolean isAuthenticated() {
		return session != null;
	}

	/** @return the authenticated display identity, if a session exists */
	public Optional<AuthenticatedUser> currentUser() {
		return Optional.ofNullable(session).map(UserSession::user);
	}

	/**
	 * @return the current credentials, if a session exists; intended for the
	 *         HTTP/service layer making authenticated requests
	 */
	public Optional<BasicCredentials> currentCredentials() {
		return Optional.ofNullable(session).map(UserSession::credentials);
	}

	/**
	 * @return the authenticated display identity
	 * @throws IllegalStateException if no session is authenticated
	 */
	public AuthenticatedUser requireCurrentUser() {
		if (session == null) {
			throw new IllegalStateException("No authenticated session is present");
		}
		return session.user();
	}

	/** Clears the session, dropping all identity and credential references. */
	public void logout() {
		this.session = null;
	}

	@Override
	public String toString() {
		return "SessionManager{authenticated=" + isAuthenticated() + "}";
	}
}

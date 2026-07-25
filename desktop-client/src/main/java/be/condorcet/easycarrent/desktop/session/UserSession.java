package be.condorcet.easycarrent.desktop.session;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;

import java.util.Objects;

/**
 * A single authenticated session held entirely in memory.
 *
 * <p>Bundles the display {@link AuthenticatedUser} with the {@link BasicCredentials}
 * needed to make further authenticated API requests. The credentials are exposed
 * only through {@link #credentials()} for the HTTP/service layer; the password
 * itself is never reachable from this class, and {@link #toString()} omits the
 * credentials entirely.</p>
 */
public final class UserSession {

	private final AuthenticatedUser user;
	private final BasicCredentials credentials;

	public UserSession(AuthenticatedUser user, BasicCredentials credentials) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
	}

	public AuthenticatedUser user() {
		return user;
	}

	/**
	 * @return the credentials for authenticated requests; intended for the
	 *         HTTP/service layer only
	 */
	public BasicCredentials credentials() {
		return credentials;
	}

	@Override
	public String toString() {
		return "UserSession{username='" + user.username() + "', role=" + user.role() + "}";
	}
}

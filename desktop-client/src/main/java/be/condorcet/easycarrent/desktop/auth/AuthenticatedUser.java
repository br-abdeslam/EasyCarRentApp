package be.condorcet.easycarrent.desktop.auth;

import java.util.Objects;

/**
 * Immutable display identity of an authenticated user.
 *
 * <p>Contains only the information safe to show in the UI: the username and the
 * resolved {@link DesktopUserRole}. It never holds the password.</p>
 *
 * @param username the authenticated username
 * @param role     the resolved role
 */
public record AuthenticatedUser(String username, DesktopUserRole role) {

	public AuthenticatedUser {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username must not be blank");
		}
		Objects.requireNonNull(role, "role must not be null");
	}
}

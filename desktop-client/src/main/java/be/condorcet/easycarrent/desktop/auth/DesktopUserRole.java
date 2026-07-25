package be.condorcet.easycarrent.desktop.auth;

import java.util.Optional;

/**
 * Roles supported by the desktop client, matching the backend security roles.
 */
public enum DesktopUserRole {

	USER,
	ADMIN;

	/**
	 * Resolves the role for a fixed development account after the backend has
	 * already authenticated the credentials.
	 *
	 * <p><strong>Development only.</strong> The backend exposes no authenticated
	 * identity endpoint, so the role is mapped from the fixed in-memory accounts
	 * declared in the backend {@code SecurityConfig} ({@code user} &rarr; USER,
	 * {@code admin} &rarr; ADMIN). This mapping is valid solely for the current
	 * development Basic-auth configuration and must be replaced by a backend
	 * identity endpoint if the accounts ever become dynamic.</p>
	 *
	 * @param username an already-authenticated username
	 * @return the mapped role, or empty if the username is not a known account
	 */
	public static Optional<DesktopUserRole> forDevelopmentUsername(String username) {
		if (username == null) {
			return Optional.empty();
		}
		return switch (username) {
			case "user" -> Optional.of(USER);
			case "admin" -> Optional.of(ADMIN);
			default -> Optional.empty();
		};
	}
}

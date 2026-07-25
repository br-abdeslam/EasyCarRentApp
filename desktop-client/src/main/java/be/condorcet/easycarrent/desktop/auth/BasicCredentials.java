package be.condorcet.easycarrent.desktop.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * In-memory HTTP Basic credentials.
 *
 * <p>Holds a username and password only for the lifetime of the application and
 * never persists them. The password is deliberately not exposed through a
 * getter: it is reachable only via {@link #toAuthorizationHeader()}, which the
 * HTTP layer uses to build the {@code Authorization} header. {@link #toString()}
 * never includes the password or the encoded header, so credentials cannot leak
 * through logs or exception messages.</p>
 */
public final class BasicCredentials {

	private final String username;
	private final String password;

	public BasicCredentials(String username, String password) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username must not be blank");
		}
		this.username = username;
		this.password = Objects.requireNonNull(password, "password must not be null");
	}

	public String username() {
		return username;
	}

	/**
	 * Builds the {@code Authorization} header value for these credentials.
	 *
	 * <p>This is the only access path to the password and is intended solely for
	 * the HTTP layer. The value is never logged or placed in exceptions.</p>
	 *
	 * @return a value of the form {@code Basic <base64(username:password)>}
	 */
	public String toAuthorizationHeader() {
		String raw = username + ":" + password;
		String encoded = Base64.getEncoder()
				.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}

	@Override
	public String toString() {
		return "BasicCredentials{username='" + username + "'}";
	}
}

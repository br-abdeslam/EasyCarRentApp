package be.condorcet.easycarrent.desktop.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class BasicCredentialsTest {

	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	@Test
	void createsWithValidValues() {
		BasicCredentials credentials = new BasicCredentials(USERNAME, PASSWORD);

		assertEquals(USERNAME, credentials.username());
	}

	@Test
	void rejectsBlankUsername() {
		assertThrows(IllegalArgumentException.class,
				() -> new BasicCredentials("   ", PASSWORD));
	}

	@Test
	void rejectsNullUsername() {
		assertThrows(IllegalArgumentException.class,
				() -> new BasicCredentials(null, PASSWORD));
	}

	@Test
	void rejectsNullPassword() {
		assertThrows(NullPointerException.class,
				() -> new BasicCredentials(USERNAME, null));
	}

	@Test
	void authorizationHeaderUsesUtf8Base64() {
		BasicCredentials credentials = new BasicCredentials(USERNAME, PASSWORD);
		String expected = "Basic " + Base64.getEncoder().encodeToString(
				(USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

		assertEquals(expected, credentials.toAuthorizationHeader());
	}

	@Test
	void toStringNeverExposesPasswordOrHeader() {
		BasicCredentials credentials = new BasicCredentials(USERNAME, PASSWORD);
		String text = credentials.toString();

		assertFalse(text.contains(PASSWORD), "toString must not contain the password");
		assertFalse(text.contains("Basic "), "toString must not contain an Authorization value");
		assertTrue(text.contains(USERNAME), "toString may contain the username");
	}
}

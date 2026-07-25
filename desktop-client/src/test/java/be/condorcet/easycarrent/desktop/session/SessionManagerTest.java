package be.condorcet.easycarrent.desktop.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;

import org.junit.jupiter.api.Test;

class SessionManagerTest {

	private static final String USERNAME = "test-user";
	private static final String PASSWORD = "fictional-password";

	private AuthenticatedUser user() {
		return new AuthenticatedUser(USERNAME, DesktopUserRole.USER);
	}

	private BasicCredentials credentials() {
		return new BasicCredentials(USERNAME, PASSWORD);
	}

	@Test
	void isUnauthenticatedInitially() {
		SessionManager manager = new SessionManager();

		assertFalse(manager.isAuthenticated());
		assertTrue(manager.currentUser().isEmpty());
		assertTrue(manager.currentCredentials().isEmpty());
	}

	@Test
	void startCreatesAuthenticatedSessionExposingIdentity() {
		SessionManager manager = new SessionManager();

		manager.start(user(), credentials());

		assertTrue(manager.isAuthenticated());
		assertEquals(USERNAME, manager.currentUser().orElseThrow().username());
		assertEquals(DesktopUserRole.USER, manager.currentUser().orElseThrow().role());
	}

	@Test
	void exposesCredentialsForHttpLayer() {
		SessionManager manager = new SessionManager();

		manager.start(user(), credentials());

		assertTrue(manager.currentCredentials().isPresent());
		assertEquals(USERNAME, manager.currentCredentials().orElseThrow().username());
	}

	@Test
	void logoutClearsIdentityAndCredentials() {
		SessionManager manager = new SessionManager();
		manager.start(user(), credentials());

		manager.logout();

		assertFalse(manager.isAuthenticated());
		assertTrue(manager.currentUser().isEmpty());
		assertTrue(manager.currentCredentials().isEmpty());
	}

	@Test
	void requireCurrentUserRejectsWhenUnauthenticated() {
		SessionManager manager = new SessionManager();

		assertThrows(IllegalStateException.class, manager::requireCurrentUser);
	}

	@Test
	void toStringNeverExposesPassword() {
		SessionManager manager = new SessionManager();
		manager.start(user(), credentials());

		assertFalse(manager.toString().contains(PASSWORD));
		assertFalse(new UserSession(user(), credentials()).toString().contains(PASSWORD));
	}
}

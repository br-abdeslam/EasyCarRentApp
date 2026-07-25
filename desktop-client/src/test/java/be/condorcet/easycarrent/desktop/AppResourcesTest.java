package be.condorcet.easycarrent.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Focused, non-UI tests that verify the desktop client's FXML/CSS resources are
 * present and well-formed. These tests never initialize the JavaFX toolkit and
 * never open a Stage.
 */
class AppResourcesTest {

	private static final String MAIN_VIEW_FXML =
			"/be/condorcet/easycarrent/desktop/view/main-view.fxml";
	private static final String LOGIN_VIEW_FXML =
			"/be/condorcet/easycarrent/desktop/view/login-view.fxml";
	private static final String APP_STYLESHEET =
			"/be/condorcet/easycarrent/desktop/view/app.css";
	private static final String DESKTOP_PROPERTIES =
			"/be/condorcet/easycarrent/desktop/config/desktop.properties";

	@Test
	void mainViewFxmlExistsOnClasspath() {
		assertNotNull(resource(MAIN_VIEW_FXML),
				"main-view.fxml must exist on the test classpath");
	}

	@Test
	void appStylesheetExistsOnClasspath() {
		assertNotNull(resource(APP_STYLESHEET),
				"app.css must exist on the test classpath");
	}

	@Test
	void mainViewFxmlIsWellFormedXml() throws Exception {
		try (InputStream in = stream(MAIN_VIEW_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// The FXML uses processing instructions for imports; do not resolve them.
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"main-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void mainViewFxmlDeclaresExpectedController() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(
				fxml.contains(
						"fx:controller=\"be.condorcet.easycarrent.desktop.view.MainViewController\""),
				"main-view.fxml must declare MainViewController as its controller");
	}

	@Test
	void mainViewFxmlContainsStatusLabelId() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"statusLabel\""),
				"main-view.fxml must contain the statusLabel fx:id");
	}

	@Test
	void appStylesheetContainsRequiredClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		assertTrue(css.contains(".app-root"), "app.css must define .app-root");
		assertTrue(css.contains(".app-title"), "app.css must define .app-title");
		assertTrue(css.contains(".app-status"), "app.css must define .app-status");
	}

	@Test
	void desktopPropertiesExistsOnClasspath() {
		assertNotNull(resource(DESKTOP_PROPERTIES),
				"desktop.properties must exist on the test classpath");
	}

	@Test
	void mainViewFxmlContainsBackendStatusLabelId() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"backendStatusLabel\""),
				"main-view.fxml must contain the backendStatusLabel fx:id");
	}

	@Test
	void appStylesheetContainsBackendStatusClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		assertTrue(css.contains(".backend-status"),
				"app.css must define .backend-status");
		assertTrue(css.contains(".backend-status-pending"),
				"app.css must define .backend-status-pending");
		assertTrue(css.contains(".backend-status-connected"),
				"app.css must define .backend-status-connected");
		assertTrue(css.contains(".backend-status-unavailable"),
				"app.css must define .backend-status-unavailable");
	}

	@Test
	void loginViewFxmlExistsOnClasspath() {
		assertNotNull(resource(LOGIN_VIEW_FXML),
				"login-view.fxml must exist on the test classpath");
	}

	@Test
	void loginViewFxmlIsWellFormedXml() throws Exception {
		try (InputStream in = stream(LOGIN_VIEW_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"login-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void loginViewDeclaresLoginController() throws Exception {
		String fxml = readResource(LOGIN_VIEW_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.LoginController\""),
				"login-view.fxml must declare LoginController");
	}

	@Test
	void loginViewContainsExpectedFieldIds() throws Exception {
		String fxml = readResource(LOGIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"usernameField\""), "usernameField required");
		assertTrue(fxml.contains("fx:id=\"passwordField\""), "passwordField required");
		assertTrue(fxml.contains("fx:id=\"loginButton\""), "loginButton required");
		assertTrue(fxml.contains("fx:id=\"messageLabel\""), "messageLabel required");
		assertTrue(fxml.contains("fx:id=\"progressIndicator\""), "progressIndicator required");
	}

	@Test
	void loginViewUsesPasswordFieldForPassword() throws Exception {
		String fxml = readResource(LOGIN_VIEW_FXML);
		assertTrue(fxml.contains("<PasswordField"),
				"the password must use a PasswordField");
	}

	@Test
	void loginViewHasNoDefaultUsernameValue() throws Exception {
		String usernameLine = lineContaining(LOGIN_VIEW_FXML, "fx:id=\"usernameField\"");
		assertFalse(usernameLine.contains("text=\""),
				"usernameField must not define a default text value");
	}

	@Test
	void loginViewHasNoDefaultPasswordValue() throws Exception {
		String passwordLine = lineContaining(LOGIN_VIEW_FXML, "fx:id=\"passwordField\"");
		assertFalse(passwordLine.contains("text=\""),
				"passwordField must not define a default text value");
	}

	@Test
	void mainViewContainsSessionControls() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"usernameLabel\""), "usernameLabel required");
		assertTrue(fxml.contains("fx:id=\"roleLabel\""), "roleLabel required");
		assertTrue(fxml.contains("fx:id=\"logoutButton\""), "logoutButton required");
	}

	@Test
	void appStylesheetContainsLoginAndSessionClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		assertTrue(css.contains(".login-card"), "app.css must define .login-card");
		assertTrue(css.contains(".login-title"), "app.css must define .login-title");
		assertTrue(css.contains(".login-button"), "app.css must define .login-button");
		assertTrue(css.contains(".login-message"), "app.css must define .login-message");
		assertTrue(css.contains(".session-username"), "app.css must define .session-username");
		assertTrue(css.contains(".session-role"), "app.css must define .session-role");
		assertTrue(css.contains(".logout-button"), "app.css must define .logout-button");
	}

	@Test
	void fxmlFilesUseNoInlineStyleAttribute() throws Exception {
		assertFalse(readResource(LOGIN_VIEW_FXML).contains("style=\""),
				"login-view.fxml must not use inline style attributes");
		assertFalse(readResource(MAIN_VIEW_FXML).contains("style=\""),
				"main-view.fxml must not use inline style attributes");
	}

	private static String lineContaining(String path, String token) throws Exception {
		for (String line : readResource(path).split("\\R")) {
			if (line.contains(token)) {
				return line;
			}
		}
		throw new AssertionError("No line containing '" + token + "' in " + path);
	}

	private static URL resource(String path) {
		return AppResourcesTest.class.getResource(path);
	}

	private static InputStream stream(String path) {
		InputStream in = AppResourcesTest.class.getResourceAsStream(path);
		assertNotNull(in, () -> "Resource not found on classpath: " + path);
		return in;
	}

	private static String readResource(String path) throws Exception {
		try (InputStream in = stream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}

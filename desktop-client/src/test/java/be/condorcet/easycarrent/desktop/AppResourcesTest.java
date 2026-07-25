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
	private static final String SECTION_PLACEHOLDER_FXML =
			"/be/condorcet/easycarrent/desktop/view/section-placeholder.fxml";
	private static final String VEHICLE_CATEGORIES_FXML =
			"/be/condorcet/easycarrent/desktop/view/vehicle-categories-view.fxml";
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
		assertFalse(readResource(SECTION_PLACEHOLDER_FXML).contains("style=\""),
				"section-placeholder.fxml must not use inline style attributes");
		assertFalse(readResource(VEHICLE_CATEGORIES_FXML).contains("style=\""),
				"vehicle-categories-view.fxml must not use inline style attributes");
	}

	@Test
	void vehicleCategoriesFxmlExistsAndIsWellFormed() throws Exception {
		assertNotNull(resource(VEHICLE_CATEGORIES_FXML),
				"vehicle-categories-view.fxml must exist on the test classpath");
		try (InputStream in = stream(VEHICLE_CATEGORIES_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"vehicle-categories-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void vehicleCategoriesFxmlDeclaresController() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.VehicleCategoryController\""),
				"vehicle-categories-view.fxml must declare VehicleCategoryController");
	}

	@Test
	void vehicleCategoriesFxmlContainsTableAndColumns() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertTrue(fxml.contains("fx:id=\"categoryTable\""), "categoryTable required");
		assertTrue(fxml.contains("fx:id=\"idColumn\""), "idColumn required");
		assertTrue(fxml.contains("fx:id=\"nameColumn\""), "nameColumn required");
		assertTrue(fxml.contains("fx:id=\"descriptionColumn\""), "descriptionColumn required");
	}

	@Test
	void vehicleCategoriesFxmlContainsActionControls() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertTrue(fxml.contains("fx:id=\"refreshButton\""), "refreshButton required");
		assertTrue(fxml.contains("fx:id=\"addButton\""), "addButton required");
		assertTrue(fxml.contains("fx:id=\"editButton\""), "editButton required");
		assertTrue(fxml.contains("fx:id=\"deleteButton\""), "deleteButton required");
	}

	@Test
	void vehicleCategoriesFxmlContainsStateControls() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertTrue(fxml.contains("fx:id=\"loadingIndicator\""), "loadingIndicator required");
		assertTrue(fxml.contains("fx:id=\"statusMessageLabel\""), "statusMessageLabel required");
		assertTrue(fxml.contains("fx:id=\"emptyStateLabel\""), "emptyStateLabel required");
	}

	@Test
	void vehicleCategoriesFxmlContainsFormControls() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertTrue(fxml.contains("fx:id=\"categoryNameField\""), "categoryNameField required");
		assertTrue(fxml.contains("fx:id=\"categoryDescriptionArea\""),
				"categoryDescriptionArea required");
		assertTrue(fxml.contains("fx:id=\"saveButton\""), "saveButton required");
		assertTrue(fxml.contains("fx:id=\"cancelButton\""), "cancelButton required");
	}

	@Test
	void vehicleCategoriesFxmlHasNoStaticTableData() throws Exception {
		String fxml = readResource(VEHICLE_CATEGORIES_FXML);
		assertFalse(fxml.contains("<items>"),
				"the category table must not define hardcoded rows");
	}

	@Test
	void appStylesheetContainsCategoryClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		for (String cssClass : new String[] {
				".category-view", ".category-header", ".category-toolbar", ".category-table",
				".category-empty-state", ".category-loading", ".category-status",
				".category-status-success", ".category-status-error", ".category-editor",
				".category-editor-title", ".form-row", ".form-label", ".validation-message",
				".primary-button", ".secondary-button", ".danger-button", ".read-only-notice"}) {
			assertTrue(css.contains(cssClass), "app.css must define " + cssClass);
		}
	}

	@Test
	void sectionPlaceholderFxmlExistsOnClasspath() {
		assertNotNull(resource(SECTION_PLACEHOLDER_FXML),
				"section-placeholder.fxml must exist on the test classpath");
	}

	@Test
	void sectionPlaceholderFxmlIsWellFormedXml() throws Exception {
		try (InputStream in = stream(SECTION_PLACEHOLDER_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"section-placeholder.fxml must parse into a valid XML document");
		}
	}

	@Test
	void sectionPlaceholderDeclaresController() throws Exception {
		String fxml = readResource(SECTION_PLACEHOLDER_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.SectionPlaceholderController\""),
				"section-placeholder.fxml must declare SectionPlaceholderController");
	}

	@Test
	void sectionPlaceholderContainsLabelIds() throws Exception {
		String fxml = readResource(SECTION_PLACEHOLDER_FXML);
		assertTrue(fxml.contains("fx:id=\"sectionTitleLabel\""), "sectionTitleLabel required");
		assertTrue(fxml.contains("fx:id=\"sectionDescriptionLabel\""),
				"sectionDescriptionLabel required");
	}

	@Test
	void mainViewContainsContentHost() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"contentHost\""),
				"main-view.fxml must contain the contentHost");
	}

	@Test
	void mainViewContainsAllNavigationButtons() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("fx:id=\"dashboardNavigationButton\""), "dashboard button required");
		assertTrue(fxml.contains("fx:id=\"vehicleCategoriesNavigationButton\""),
				"vehicle categories button required");
		assertTrue(fxml.contains("fx:id=\"vehiclesNavigationButton\""), "vehicles button required");
		assertTrue(fxml.contains("fx:id=\"customersNavigationButton\""), "customers button required");
		assertTrue(fxml.contains("fx:id=\"rentalsNavigationButton\""), "rentals button required");
		assertTrue(fxml.contains("fx:id=\"paymentsNavigationButton\""), "payments button required");
		assertTrue(fxml.contains("fx:id=\"maintenanceNavigationButton\""),
				"maintenance button required");
	}

	@Test
	void mainViewContainsToggleGroup() throws Exception {
		String fxml = readResource(MAIN_VIEW_FXML);
		assertTrue(fxml.contains("<ToggleGroup"),
				"main-view.fxml must define a ToggleGroup for navigation");
		assertTrue(fxml.contains("fx:id=\"navigationToggleGroup\""),
				"the navigation ToggleGroup must have an fx:id");
	}

	@Test
	void mainViewDashboardIsInitiallySelected() throws Exception {
		String dashboardLine =
				lineContaining(MAIN_VIEW_FXML, "fx:id=\"dashboardNavigationButton\"");
		assertTrue(dashboardLine.contains("selected=\"true\""),
				"the Dashboard navigation button must be selected by default");
	}

	@Test
	void mainViewRetainsBackendStatusLabel() throws Exception {
		assertTrue(readResource(MAIN_VIEW_FXML).contains("fx:id=\"backendStatusLabel\""),
				"backendStatusLabel must remain present in the shell");
	}

	@Test
	void appStylesheetContainsNavigationClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		assertTrue(css.contains(".main-shell"), "app.css must define .main-shell");
		assertTrue(css.contains(".app-header"), "app.css must define .app-header");
		assertTrue(css.contains(".navigation-sidebar"), "app.css must define .navigation-sidebar");
		assertTrue(css.contains(".navigation-button"), "app.css must define .navigation-button");
		assertTrue(css.contains(".navigation-button:selected"),
				"app.css must define the selected navigation state");
		assertTrue(css.contains(".content-host"), "app.css must define .content-host");
		assertTrue(css.contains(".section-placeholder"), "app.css must define .section-placeholder");
		assertTrue(css.contains(".section-placeholder-title"),
				"app.css must define .section-placeholder-title");
		assertTrue(css.contains(".section-placeholder-description"),
				"app.css must define .section-placeholder-description");
		assertTrue(css.contains(".app-status-bar"), "app.css must define .app-status-bar");
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

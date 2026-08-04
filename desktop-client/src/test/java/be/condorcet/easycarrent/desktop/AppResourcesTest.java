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
	private static final String VEHICLES_FXML =
			"/be/condorcet/easycarrent/desktop/view/vehicles-view.fxml";
	private static final String CUSTOMERS_FXML =
			"/be/condorcet/easycarrent/desktop/view/customers-view.fxml";
	private static final String RENTALS_FXML =
			"/be/condorcet/easycarrent/desktop/view/rentals-view.fxml";
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
		assertFalse(readResource(VEHICLES_FXML).contains("style=\""),
				"vehicles-view.fxml must not use inline style attributes");
		assertFalse(readResource(CUSTOMERS_FXML).contains("style=\""),
				"customers-view.fxml must not use inline style attributes");
		assertFalse(readResource(RENTALS_FXML).contains("style=\""),
				"rentals-view.fxml must not use inline style attributes");
	}

	@Test
	void customersFxmlExistsAndIsWellFormed() throws Exception {
		assertNotNull(resource(CUSTOMERS_FXML),
				"customers-view.fxml must exist on the test classpath");
		try (InputStream in = stream(CUSTOMERS_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"customers-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void customersFxmlDeclaresController() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.CustomerController\""),
				"customers-view.fxml must declare CustomerController");
	}

	@Test
	void customersFxmlContainsTableAndColumns() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		assertTrue(fxml.contains("fx:id=\"customerTable\""), "customerTable required");
		for (String column : new String[] {"idColumn", "firstNameColumn", "lastNameColumn",
				"emailColumn", "phoneColumn", "drivingLicenseColumn", "drivingLicenseExpiryColumn"}) {
			assertTrue(fxml.contains("fx:id=\"" + column + "\""), column + " required");
		}
	}

	@Test
	void customersFxmlExpiryColumnHasReadableHeading() throws Exception {
		String expiryLine = lineContaining(CUSTOMERS_FXML, "fx:id=\"drivingLicenseExpiryColumn\"");
		assertTrue(expiryLine.contains("text=\"Licence expiry\""),
				"the licence-expiry column must have a readable heading");
	}

	@Test
	void customersUseAnExpandableValidationMessagesContainer() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		String containerLine =
				lineContaining(CUSTOMERS_FXML, "fx:id=\"validationMessagesContainer\"");
		assertTrue(containerLine.contains("<VBox"),
				"validation errors must render in an expandable VBox container");
		assertFalse(containerLine.contains("prefHeight") || containerLine.contains("maxHeight"),
				"the validation container must not have a fixed height");
		assertFalse(fxml.contains("fx:id=\"validationMessageLabel\""),
				"the obsolete single-line validation label must be removed");
		assertFalse(fxml.contains("textOverrun"),
				"customers-view.fxml must not force ellipsis truncation");
	}

	@Test
	void customersStatusLabelWraps() throws Exception {
		assertTrue(lineContaining(CUSTOMERS_FXML, "fx:id=\"statusMessageLabel\"")
				.contains("wrapText=\"true\""),
				"the status label must wrap so long messages are readable");
	}

	@Test
	void customersFxmlContainsActionControls() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		for (String id : new String[] {"refreshButton", "addButton", "editButton", "deleteButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void customersFxmlContainsStateControls() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		for (String id : new String[] {"loadingIndicator", "statusMessageLabel", "emptyStateLabel",
				"readOnlyNoticeLabel"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void customersFxmlContainsFormControls() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		for (String id : new String[] {"firstNameField", "lastNameField", "emailField", "phoneField",
				"addressArea", "drivingLicenseField", "drivingLicenseExpiryPicker", "saveButton",
				"cancelButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void customersFxmlHasNoPasswordFieldOrStaticData() throws Exception {
		String fxml = readResource(CUSTOMERS_FXML);
		assertFalse(fxml.contains("<PasswordField"),
				"customers-view.fxml must not contain a password field");
		assertFalse(fxml.contains("<items>"),
				"the customer table must not define hardcoded rows");
	}

	@Test
	void appStylesheetContainsCustomerClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		for (String cssClass : new String[] {
				".customer-view", ".customer-header", ".customer-toolbar", ".customer-table",
				".customer-empty-state", ".customer-loading", ".customer-status",
				".customer-status-success", ".customer-status-error", ".customer-editor",
				".customer-editor-title", ".customer-form-field", ".customer-validation-message",
				".customer-validation-messages"}) {
			assertTrue(css.contains(cssClass), "app.css must define " + cssClass);
		}
	}

	@Test
	void rentalsFxmlExistsAndIsWellFormed() throws Exception {
		assertNotNull(resource(RENTALS_FXML),
				"rentals-view.fxml must exist on the test classpath");
		try (InputStream in = stream(RENTALS_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"rentals-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void rentalsFxmlDeclaresController() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.RentalController\""),
				"rentals-view.fxml must declare RentalController");
	}

	@Test
	void rentalsFxmlContainsTableAndColumns() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		assertTrue(fxml.contains("fx:id=\"rentalTable\""), "rentalTable required");
		for (String column : new String[] {"idColumn", "customerColumn", "vehicleColumn",
				"startDateColumn", "endDateColumn", "statusColumn", "totalPriceColumn"}) {
			assertTrue(fxml.contains("fx:id=\"" + column + "\""), column + " required");
		}
	}

	@Test
	void rentalsFxmlContainsSupportedActionControls() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		for (String id : new String[] {"refreshButton", "addButton", "editButton",
				"startRentalButton", "completeRentalButton", "cancelRentalButton", "deleteButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void rentalsFxmlHasNoUnsupportedActionControls() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		// The backend exposes no reactivate/return/reopen transitions, so no such control exists.
		for (String id : new String[] {"reactivateRentalButton", "returnRentalButton",
				"reopenRentalButton", "payButton", "invoiceButton"}) {
			assertFalse(fxml.contains("fx:id=\"" + id + "\""), id + " must not exist");
		}
	}

	@Test
	void rentalsFxmlContainsStateControls() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		for (String id : new String[] {"loadingIndicator", "statusMessageLabel", "emptyStateLabel",
				"readOnlyNoticeLabel"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void rentalsFxmlContainsEditorControls() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		for (String id : new String[] {"customerComboBox", "vehicleComboBox", "startDatePicker",
				"endDatePicker", "formMessagesContainer", "saveButton", "cancelEditButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void rentalsFxmlUsesComboBoxesForCustomerAndVehicle() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		String customerLine = lineContaining(RENTALS_FXML, "fx:id=\"customerComboBox\"");
		String vehicleLine = lineContaining(RENTALS_FXML, "fx:id=\"vehicleComboBox\"");
		assertTrue(customerLine.contains("<ComboBox"), "the customer selector must be a ComboBox");
		assertTrue(vehicleLine.contains("<ComboBox"), "the vehicle selector must be a ComboBox");
		assertFalse(customerLine.contains("editable=\"true\""),
				"the customer ComboBox must not be free-text editable");
		assertFalse(vehicleLine.contains("editable=\"true\""),
				"the vehicle ComboBox must not be free-text editable");
	}

	@Test
	void rentalsFxmlDoesNotExposeServerManagedFields() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		// Status and total price are backend-managed; there must be no editable field for them.
		assertFalse(fxml.contains("fx:id=\"statusField\""), "status must not be editable");
		assertFalse(fxml.contains("fx:id=\"statusComboBox\""), "status must not be editable");
		assertFalse(fxml.contains("fx:id=\"totalPriceField\""), "total price must not be editable");
		// The estimate is a read-only Label, not an input.
		String estimateLine = lineContaining(RENTALS_FXML, "fx:id=\"totalPricePreviewLabel\"");
		assertTrue(estimateLine.contains("<Label"), "the total-price estimate must be a read-only Label");
	}

	@Test
	void rentalsFxmlHasNoPasswordPaymentOrStaticData() throws Exception {
		String fxml = readResource(RENTALS_FXML);
		assertFalse(fxml.contains("<PasswordField"), "rentals-view.fxml must not contain a password field");
		assertFalse(fxml.toLowerCase().contains("payment"), "rentals-view.fxml must not contain payment controls");
		assertFalse(fxml.contains("<items>"), "the rental table must not define hardcoded rows");
	}

	@Test
	void rentalsStatusLabelWraps() throws Exception {
		assertTrue(lineContaining(RENTALS_FXML, "fx:id=\"statusMessageLabel\"")
				.contains("wrapText=\"true\""),
				"the status label must wrap so long messages are readable");
	}

	@Test
	void appStylesheetContainsRentalClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		for (String cssClass : new String[] {
				".rental-view", ".rental-header", ".rental-toolbar", ".rental-table",
				".rental-empty-state", ".rental-loading", ".rental-status-message",
				".rental-status-success", ".rental-status-error", ".rental-editor",
				".rental-editor-title", ".rental-form-field", ".rental-validation-messages",
				".rental-validation-message", ".rental-state-label", ".rental-price-column"}) {
			assertTrue(css.contains(cssClass), "app.css must define " + cssClass);
		}
	}

	@Test
	void vehiclesFxmlExistsAndIsWellFormed() throws Exception {
		assertNotNull(resource(VEHICLES_FXML),
				"vehicles-view.fxml must exist on the test classpath");
		try (InputStream in = stream(VEHICLES_FXML)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().parse(in);
			assertNotNull(document.getDocumentElement(),
					"vehicles-view.fxml must parse into a valid XML document");
		}
	}

	@Test
	void vehiclesFxmlDeclaresController() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		assertTrue(fxml.contains(
				"fx:controller=\"be.condorcet.easycarrent.desktop.view.VehicleController\""),
				"vehicles-view.fxml must declare VehicleController");
	}

	@Test
	void vehiclesFxmlContainsTableAndColumns() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		assertTrue(fxml.contains("fx:id=\"vehicleTable\""), "vehicleTable required");
		for (String column : new String[] {"idColumn", "registrationColumn", "brandColumn",
				"modelColumn", "yearColumn", "categoryColumn", "statusColumn", "dailyRateColumn"}) {
			assertTrue(fxml.contains("fx:id=\"" + column + "\""), column + " required");
		}
	}

	@Test
	void vehiclesFxmlContainsActionControls() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		for (String id : new String[] {"refreshButton", "addButton", "editButton", "deleteButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void vehiclesFxmlContainsStateControls() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		for (String id : new String[] {"loadingIndicator", "statusMessageLabel", "emptyStateLabel",
				"readOnlyNoticeLabel"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void vehiclesFxmlContainsFormControls() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		for (String id : new String[] {"registrationField", "brandField", "modelField",
				"manufacturingYearField", "colorField", "dailyRateField", "mileageField",
				"categoryComboBox", "saveButton", "cancelButton"}) {
			assertTrue(fxml.contains("fx:id=\"" + id + "\""), id + " required");
		}
	}

	@Test
	void vehiclesFxmlUsesComboBoxForCategory() throws Exception {
		String fxml = readResource(VEHICLES_FXML);
		assertTrue(fxml.contains("<ComboBox"), "the category selector must be a ComboBox");
	}

	@Test
	void vehiclesFxmlHasNoStaticTableData() throws Exception {
		assertFalse(readResource(VEHICLES_FXML).contains("<items>"),
				"the vehicle table must not define hardcoded rows");
	}

	@Test
	void appStylesheetContainsVehicleClasses() throws Exception {
		String css = readResource(APP_STYLESHEET);
		for (String cssClass : new String[] {
				".vehicle-view", ".vehicle-header", ".vehicle-toolbar", ".vehicle-table",
				".vehicle-empty-state", ".vehicle-loading", ".vehicle-status",
				".vehicle-status-success", ".vehicle-status-error", ".vehicle-editor",
				".vehicle-editor-title", ".vehicle-category-field"}) {
			assertTrue(css.contains(cssClass), "app.css must define " + cssClass);
		}
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

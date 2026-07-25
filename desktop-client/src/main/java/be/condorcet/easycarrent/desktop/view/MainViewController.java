package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.config.ApiConfiguration;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.navigation.MainContentRouter;
import be.condorcet.easycarrent.desktop.navigation.MainSection;
import be.condorcet.easycarrent.desktop.service.BackendHealthResult;
import be.condorcet.easycarrent.desktop.service.BackendHealthService;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

/**
 * Controller for the authenticated main shell.
 *
 * <p>Preserves the initialization confirmation, the non-blocking backend
 * connectivity check, the authenticated-session display, and logout. It adds a
 * persistent sidebar whose items route the central content through a
 * {@link MainContentRouter}; the header, sidebar, session identity, and backend
 * status remain visible while only the central content changes. Routing creates
 * no new Scene or Stage, makes no domain API calls, and blocks nothing. Session
 * collaborators are supplied through {@link #init} after loading; the password
 * is never accessed or displayed.</p>
 */
public class MainViewController {

	private static final String BASE_STATUS_CLASS = "backend-status";
	private static final String PENDING_CLASS = "backend-status-pending";
	private static final String CONNECTED_CLASS = "backend-status-connected";
	private static final String UNAVAILABLE_CLASS = "backend-status-unavailable";

	@FXML
	private Label statusLabel;

	@FXML
	private Label backendStatusLabel;

	@FXML
	private Label usernameLabel;

	@FXML
	private Label roleLabel;

	@FXML
	private Button logoutButton;

	@FXML
	private StackPane contentHost;

	@FXML
	private ToggleGroup navigationToggleGroup;

	@FXML
	private ToggleButton dashboardNavigationButton;

	@FXML
	private ToggleButton vehicleCategoriesNavigationButton;

	@FXML
	private ToggleButton vehiclesNavigationButton;

	@FXML
	private ToggleButton customersNavigationButton;

	@FXML
	private ToggleButton rentalsNavigationButton;

	@FXML
	private ToggleButton paymentsNavigationButton;

	@FXML
	private ToggleButton maintenanceNavigationButton;

	private final BackendHealthService backendHealthService;

	private MainContentRouter contentRouter;
	private SessionManager sessionManager;
	private ViewManager viewManager;
	private VehicleCategoryService vehicleCategoryService;
	private VehicleService vehicleService;

	public MainViewController() {
		this.backendHealthService =
				new BackendHealthService(new ApiClient(new ApiConfiguration().baseUri()));
	}

	/** Supplies collaborators after {@code FXMLLoader.load()} and builds navigation. */
	public void init(SessionManager sessionManager, ViewManager viewManager,
			VehicleCategoryService vehicleCategoryService, VehicleService vehicleService) {
		this.sessionManager = sessionManager;
		this.viewManager = viewManager;
		this.vehicleCategoryService = vehicleCategoryService;
		this.vehicleService = vehicleService;
		renderSession();
		setUpNavigation();
	}

	@FXML
	private void initialize() {
		statusLabel.setText("Desktop client initialized successfully");
		showPending();
		checkBackendConnectivity();
	}

	private void setUpNavigation() {
		contentRouter = new MainContentRouter(contentHost, vehicleCategoryService, vehicleService,
				sessionManager);

		wireNavigation(dashboardNavigationButton, MainSection.DASHBOARD);
		wireNavigation(vehicleCategoriesNavigationButton, MainSection.VEHICLE_CATEGORIES);
		wireNavigation(vehiclesNavigationButton, MainSection.VEHICLES);
		wireNavigation(customersNavigationButton, MainSection.CUSTOMERS);
		wireNavigation(rentalsNavigationButton, MainSection.RENTALS);
		wireNavigation(paymentsNavigationButton, MainSection.PAYMENTS);
		wireNavigation(maintenanceNavigationButton, MainSection.MAINTENANCE);

		// Keep exactly one section selected: clicking the active item must not
		// leave the sidebar with no selection.
		navigationToggleGroup.selectedToggleProperty().addListener((observable, previous, current) -> {
			if (current == null && previous != null) {
				previous.setSelected(true);
			}
		});

		contentRouter.showDefault();
	}

	private void wireNavigation(ToggleButton button, MainSection section) {
		button.setOnAction(event -> contentRouter.show(section));
	}

	private void renderSession() {
		Optional<AuthenticatedUser> current = sessionManager.currentUser();
		if (current.isEmpty()) {
			viewManager.showLogin();
			return;
		}
		AuthenticatedUser user = current.get();
		usernameLabel.setText(user.username());
		roleLabel.setText(user.role().name());
	}

	@FXML
	private void handleLogout() {
		sessionManager.logout();
		viewManager.showLogin();
	}

	private void checkBackendConnectivity() {
		backendHealthService.checkHealth()
				.whenComplete((result, throwable) -> Platform.runLater(() -> {
					if (throwable != null) {
						showUnavailable("Backend unavailable");
					} else {
						applyResult(result);
					}
				}));
	}

	private void applyResult(BackendHealthResult result) {
		if (result.isConnected()) {
			setBackendStatus(result.message(), CONNECTED_CLASS);
		} else {
			showUnavailable(result.message());
		}
	}

	private void showPending() {
		setBackendStatus("Checking backend connection...", PENDING_CLASS);
	}

	private void showUnavailable(String message) {
		setBackendStatus(message, UNAVAILABLE_CLASS);
	}

	private void setBackendStatus(String text, String stateClass) {
		backendStatusLabel.setText(text);
		backendStatusLabel.getStyleClass().setAll(BASE_STATUS_CLASS, stateClass);
	}
}

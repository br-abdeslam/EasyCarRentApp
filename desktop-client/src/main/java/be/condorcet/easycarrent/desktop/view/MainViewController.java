package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.AuthenticatedUser;
import be.condorcet.easycarrent.desktop.config.ApiConfiguration;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.service.BackendHealthResult;
import be.condorcet.easycarrent.desktop.service.BackendHealthService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the main view.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor. It preserves the initialization confirmation and the non-blocking
 * backend connectivity check, and adds authenticated-session display and logout.
 * Session collaborators are supplied through {@link #init} after loading; the
 * username and role are shown but the password is never accessed or displayed.
 * UI updates from async work use {@link Platform#runLater(Runnable)} and state is
 * conveyed through CSS classes only.</p>
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

	private final BackendHealthService backendHealthService;

	private SessionManager sessionManager;
	private ViewManager viewManager;

	public MainViewController() {
		this.backendHealthService =
				new BackendHealthService(new ApiClient(new ApiConfiguration().baseUri()));
	}

	/** Supplies session collaborators after {@code FXMLLoader.load()}. */
	public void init(SessionManager sessionManager, ViewManager viewManager) {
		this.sessionManager = sessionManager;
		this.viewManager = viewManager;
		renderSession();
	}

	@FXML
	private void initialize() {
		statusLabel.setText("Desktop client initialized successfully");
		showPending();
		checkBackendConnectivity();
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

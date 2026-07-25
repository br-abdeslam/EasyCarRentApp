package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.config.ApiConfiguration;
import be.condorcet.easycarrent.desktop.http.ApiClient;
import be.condorcet.easycarrent.desktop.service.BackendHealthResult;
import be.condorcet.easycarrent.desktop.service.BackendHealthService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the main view.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor. It keeps the initialization confirmation and adds a non-blocking
 * backend connectivity check: the health call runs asynchronously off the JavaFX
 * Application Thread, and the resulting UI update is marshalled back onto it with
 * {@link Platform#runLater(Runnable)}. Connectivity state is conveyed through CSS
 * classes only; no inline styles are used, and no HTTP work happens here.</p>
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

	private final BackendHealthService backendHealthService;

	public MainViewController() {
		this.backendHealthService =
				new BackendHealthService(new ApiClient(new ApiConfiguration().baseUri()));
	}

	@FXML
	private void initialize() {
		statusLabel.setText("Desktop client initialized successfully");
		showPending();
		checkBackendConnectivity();
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

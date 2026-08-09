package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.BasicCredentials;
import be.condorcet.easycarrent.desktop.service.AuthenticationResult;
import be.condorcet.easycarrent.desktop.service.AuthenticationService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Controller for the login view.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} immediately
 * after loading. Authentication runs asynchronously through
 * {@link AuthenticationService}; the JavaFX Application Thread is never blocked,
 * and every UI update from the async callback is marshalled with
 * {@link Platform#runLater(Runnable)}. The password is never logged, never
 * displayed, and the password field is cleared after each attempt. State is
 * conveyed through CSS classes only.</p>
 */
public class LoginController {

	private static final String MESSAGE_BASE_CLASS = "login-message";
	private static final String MESSAGE_PENDING_CLASS = "login-message-pending";
	private static final String MESSAGE_ERROR_CLASS = "login-message-error";

	@FXML
	private TextField usernameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button loginButton;

	@FXML
	private Label messageLabel;

	@FXML
	private ProgressIndicator progressIndicator;

	private AuthenticationService authenticationService;
	private ViewManager viewManager;

	/** Supplies collaborators after {@code FXMLLoader.load()}. */
	public void init(AuthenticationService authenticationService, ViewManager viewManager) {
		this.authenticationService = authenticationService;
		this.viewManager = viewManager;
	}

	@FXML
	private void initialize() {
		progressIndicator.setVisible(false);
		clearMessage();
	}

	/** Triggered by the login button and by pressing Enter in either field. */
	@FXML
	private void handleLogin() {
		String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
		String password = passwordField.getText() == null ? "" : passwordField.getText();

		if (username.isEmpty() || password.isEmpty()) {
			showError("Enter both a username and a password.");
			return;
		}

		BasicCredentials credentials = new BasicCredentials(username, password);
		setPending();

		authenticationService.authenticate(credentials)
				.whenComplete((result, throwable) -> Platform.runLater(() -> {
					if (throwable != null) {
						onFailure("An unexpected error occurred.");
					} else {
						onResult(result);
					}
				}));
	}

	private void onResult(AuthenticationResult result) {
		if (result.isAuthenticated()) {
			passwordField.clear();
			clearMessage();
			viewManager.showMain();
		} else {
			onFailure(result.message());
		}
	}

	private void onFailure(String message) {
		passwordField.clear();
		setControlsDisabled(false);
		progressIndicator.setVisible(false);
		showError(message);
	}

	private void setPending() {
		setControlsDisabled(true);
		progressIndicator.setVisible(true);
		messageLabel.setText("Signing in...");
		messageLabel.getStyleClass().setAll(MESSAGE_BASE_CLASS, MESSAGE_PENDING_CLASS);
	}

	private void setControlsDisabled(boolean disabled) {
		usernameField.setDisable(disabled);
		passwordField.setDisable(disabled);
		loginButton.setDisable(disabled);
	}

	private void showError(String message) {
		messageLabel.setText(message);
		messageLabel.getStyleClass().setAll(MESSAGE_BASE_CLASS, MESSAGE_ERROR_CLASS);
	}

	private void clearMessage() {
		messageLabel.setText("");
		messageLabel.getStyleClass().setAll(MESSAGE_BASE_CLASS);
	}
}

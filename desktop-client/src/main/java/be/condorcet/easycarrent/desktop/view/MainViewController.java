package be.condorcet.easycarrent.desktop.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the main view.
 *
 * <p>This controller only proves that the FXML wiring works: it is instantiated
 * by {@link javafx.fxml.FXMLLoader}, its {@link #statusLabel} field is injected,
 * and {@link #initialize()} is invoked after loading. It holds no Stage,
 * repository, service, HTTP client, or business logic.</p>
 */
public class MainViewController {

	@FXML
	private Label statusLabel;

	@FXML
	private void initialize() {
		statusLabel.setText("Desktop client initialized successfully");
	}
}

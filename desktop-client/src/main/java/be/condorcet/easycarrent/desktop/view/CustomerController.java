package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.CustomerPermissions;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;
import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.CustomerValidator;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller for the Customers screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after
 * loading, which also triggers the first asynchronous load. All API work runs
 * through {@link CustomerService}; there is no direct HTTP, no blocking call, and
 * every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. Write controls are shown only when the
 * authenticated role may write; the backend remains authoritative. Customer data
 * is never logged, and only the fields the screen needs are displayed. State is
 * conveyed through CSS classes.</p>
 */
public class CustomerController {

	private static final String STATUS_BASE = "customer-status";
	private static final String STATUS_SUCCESS = "customer-status-success";
	private static final String STATUS_ERROR = "customer-status-error";

	@FXML
	private TableView<CustomerResponseDto> customerTable;

	@FXML
	private TableColumn<CustomerResponseDto, Long> idColumn;

	@FXML
	private TableColumn<CustomerResponseDto, String> firstNameColumn;

	@FXML
	private TableColumn<CustomerResponseDto, String> lastNameColumn;

	@FXML
	private TableColumn<CustomerResponseDto, String> emailColumn;

	@FXML
	private TableColumn<CustomerResponseDto, String> phoneColumn;

	@FXML
	private TableColumn<CustomerResponseDto, String> drivingLicenseColumn;

	@FXML
	private Button refreshButton;

	@FXML
	private Button addButton;

	@FXML
	private Button editButton;

	@FXML
	private Button deleteButton;

	@FXML
	private ProgressIndicator loadingIndicator;

	@FXML
	private Label statusMessageLabel;

	@FXML
	private Label emptyStateLabel;

	@FXML
	private Label readOnlyNoticeLabel;

	@FXML
	private VBox customerEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private TextField firstNameField;

	@FXML
	private TextField lastNameField;

	@FXML
	private TextField emailField;

	@FXML
	private TextField phoneField;

	@FXML
	private TextArea addressArea;

	@FXML
	private TextField drivingLicenseField;

	@FXML
	private DatePicker drivingLicenseExpiryPicker;

	@FXML
	private Label validationMessageLabel;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelButton;

	private CustomerService customerService;
	private CustomerViewState state;

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(CustomerService customerService, SessionManager sessionManager) {
		this.customerService = Objects.requireNonNull(customerService, "customerService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser().map(user -> user.role()).orElse(null);
		this.state = new CustomerViewState(CustomerPermissions.canWrite(role));

		configureTable();
		customerTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});

		refreshControls();
		loadCustomers();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		firstNameColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().firstName()));
		lastNameColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().lastName()));
		emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().email()));
		phoneColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().phone()));
		drivingLicenseColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().drivingLicenseNumber()));
	}

	// --- Loading ---------------------------------------------------------------

	private void loadCustomers() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		clearStatus();
		refreshControls();

		customerService.findAll().whenComplete((customers, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						showStatusError(safeMessage(throwable));
					} else {
						state.loadSucceeded(customers);
						renderCustomers();
					}
					refreshControls();
				}));
	}

	private void renderCustomers() {
		customerTable.setItems(FXCollections.observableArrayList(state.customers()));
		state.selected().ifPresentOrElse(
				selected -> customerTable.getSelectionModel().select(selected),
				() -> customerTable.getSelectionModel().clearSelection());
	}

	@FXML
	private void handleRefresh() {
		loadCustomers();
	}

	// --- Create / edit ---------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Customer");
		clearFormFields();
		clearValidation();
		clearStatus();
		refreshControls();
		firstNameField.requestFocus();
	}

	@FXML
	private void handleEdit() {
		CustomerResponseDto selected = customerTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.beginEdit()) {
			return;
		}
		formTitleLabel.setText("Edit Customer");
		firstNameField.setText(selected.firstName());
		lastNameField.setText(selected.lastName());
		emailField.setText(selected.email());
		phoneField.setText(selected.phone());
		addressArea.setText(selected.address());
		drivingLicenseField.setText(selected.drivingLicenseNumber());
		drivingLicenseExpiryPicker.setValue(selected.drivingLicenseExpiryDate());
		clearValidation();
		clearStatus();
		refreshControls();
		firstNameField.requestFocus();
	}

	@FXML
	private void handleCancel() {
		state.cancelForm();
		clearValidation();
		refreshControls();
	}

	@FXML
	private void handleSave() {
		CustomerValidator.Result result = CustomerValidator.validate(
				firstNameField.getText(), lastNameField.getText(), emailField.getText(),
				phoneField.getText(), addressArea.getText(), drivingLicenseField.getText(),
				drivingLicenseExpiryPicker.getValue(), LocalDate.now());
		if (!result.isValid()) {
			showValidation(String.join("\n", result.errors()));
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		clearValidation();
		refreshControls();

		CustomerRequestDto request = result.request();
		boolean creating = state.mode() == CustomerViewState.Mode.CREATING;
		CompletableFuture<CustomerResponseDto> future = creating
				? customerService.create(request)
				: customerService.update(state.selected().orElseThrow().id(), request);

		future.whenComplete((saved, throwable) -> Platform.runLater(() -> {
			state.endOperation();
			if (throwable != null) {
				showValidation(safeMessage(throwable));
				refreshControls();
			} else {
				state.cancelForm();
				clearValidation();
				showStatusSuccess(creating ? "Customer created." : "Customer updated.");
				if (saved != null) {
					state.select(saved);
				}
				loadCustomers();
			}
		}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		CustomerResponseDto selected = customerTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}
		String displayName = (selected.firstName() + " " + selected.lastName()).trim();
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete customer \"" + displayName + "\"? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete customer");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deleteCustomer(selected));
	}

	private void deleteCustomer(CustomerResponseDto customer) {
		if (!state.beginOperation()) {
			return;
		}
		clearStatus();
		refreshControls();

		customerService.delete(customer.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						showStatusError(safeMessage(throwable));
						refreshControls();
					} else {
						state.clearSelection();
						showStatusSuccess("Customer deleted.");
						loadCustomers();
					}
				}));
	}

	// --- Control state ---------------------------------------------------------

	private void refreshControls() {
		boolean readOnly = state.isReadOnly();
		setVisibleManaged(readOnlyNoticeLabel, readOnly);
		setVisibleManaged(addButton, !readOnly);
		setVisibleManaged(editButton, !readOnly);
		setVisibleManaged(deleteButton, !readOnly);

		addButton.setDisable(!state.canCreate());
		editButton.setDisable(!state.canEdit());
		deleteButton.setDisable(!state.canDelete());
		refreshButton.setDisable(!state.canRefresh());
		saveButton.setDisable(state.isBusy());
		cancelButton.setDisable(state.isBusy());

		loadingIndicator.setVisible(state.isLoading());
		setVisibleManaged(emptyStateLabel, state.isEmpty());
		setVisibleManaged(customerEditor, state.isFormVisible());
	}

	private void clearFormFields() {
		firstNameField.clear();
		lastNameField.clear();
		emailField.clear();
		phoneField.clear();
		addressArea.clear();
		drivingLicenseField.clear();
		drivingLicenseExpiryPicker.setValue(null);
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	// --- Messages --------------------------------------------------------------

	private void showStatusSuccess(String message) {
		statusMessageLabel.setText(message);
		statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_SUCCESS);
		setVisibleManaged(statusMessageLabel, true);
	}

	private void showStatusError(String message) {
		statusMessageLabel.setText(message);
		statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_ERROR);
		setVisibleManaged(statusMessageLabel, true);
	}

	private void clearStatus() {
		statusMessageLabel.setText("");
		statusMessageLabel.getStyleClass().setAll(STATUS_BASE);
		setVisibleManaged(statusMessageLabel, false);
	}

	private void showValidation(String message) {
		validationMessageLabel.setText(message);
		setVisibleManaged(validationMessageLabel, true);
	}

	private void clearValidation() {
		validationMessageLabel.setText("");
		setVisibleManaged(validationMessageLabel, false);
	}

	private String safeMessage(Throwable throwable) {
		Throwable cause = unwrap(throwable);
		if (cause instanceof ApiConnectionException) {
			return "The backend is unavailable. Please try again.";
		}
		if (cause instanceof ApiRequestException request) {
			return safeRequestMessage(request);
		}
		return "An unexpected error occurred.";
	}

	private String safeRequestMessage(ApiRequestException request) {
		int status = request.status();
		if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
			return request.apiError()
					.map(this::formatValidationErrors)
					.orElse("The request was invalid.");
		}
		if (status == HttpURLConnection.HTTP_UNAUTHORIZED
				|| status == HttpURLConnection.HTTP_FORBIDDEN) {
			return "You are not authorized to perform this action.";
		}
		if (status == HttpURLConnection.HTTP_NOT_FOUND) {
			return "The customer no longer exists. Please refresh.";
		}
		if (status == HttpURLConnection.HTTP_CONFLICT) {
			return request.apiError()
					.map(ApiErrorDto::message)
					.filter(message -> message != null && !message.isBlank())
					.orElse("The request conflicts with existing data.");
		}
		return "Unexpected response from the backend (status " + status + ").";
	}

	private String formatValidationErrors(ApiErrorDto error) {
		if (error.validationErrors() != null && !error.validationErrors().isEmpty()) {
			return error.validationErrors().entrySet().stream()
					.map(entry -> entry.getKey() + ": " + entry.getValue())
					.collect(Collectors.joining("\n"));
		}
		return error.message() == null || error.message().isBlank()
				? "The request was invalid."
				: error.message();
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}

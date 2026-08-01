package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.CustomerPermissions;
import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.CustomerRequestDto;
import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.service.CustomerDateFormatter;
import be.condorcet.easycarrent.desktop.service.CustomerMessages;
import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.CustomerValidator;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
	private TableColumn<CustomerResponseDto, String> drivingLicenseExpiryColumn;

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
	private VBox validationMessagesContainer;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelButton;

	private CustomerService customerService;
	private CustomerViewState state;
	private final CustomerMessageState messages = new CustomerMessageState();

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
		renderMessages();
		loadCustomersData();
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
		drivingLicenseExpiryColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				CustomerDateFormatter.formatExpiry(cell.getValue().drivingLicenseExpiryDate())));
	}

	// --- Loading ---------------------------------------------------------------

	/**
	 * Loads the list from the backend. This only touches the list and the general
	 * status area on failure; it never clears a message the caller has just set (so a
	 * post-save success message survives the subsequent reload).
	 */
	private void loadCustomersData() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		refreshControls();

		customerService.findAll().whenComplete((customers, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						messages.statusError(CustomerMessages.forLoadFailure(throwable));
						renderMessages();
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
		messages.clearAll();
		renderMessages();
		loadCustomersData();
	}

	// --- Create / edit ---------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Customer");
		clearFormFields();
		messages.clearAll();
		renderMessages();
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
		messages.clearAll();
		renderMessages();
		refreshControls();
		firstNameField.requestFocus();
	}

	@FXML
	private void handleCancel() {
		state.cancelForm();
		messages.clearAll();
		renderMessages();
		refreshControls();
	}

	@FXML
	private void handleSave() {
		// Every Save attempt starts from a clean slate: no stale form or status message.
		messages.clearAll();
		renderMessages();

		CustomerValidator.Result result = CustomerValidator.validate(
				firstNameField.getText(), lastNameField.getText(), emailField.getText(),
				phoneField.getText(), addressArea.getText(), drivingLicenseField.getText(),
				drivingLicenseExpiryPicker.getValue(), LocalDate.now());
		if (!result.isValid()) {
			messages.formErrors(CustomerMessages.localValidationLines(result.errors()));
			renderMessages();
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		refreshControls();

		CustomerRequestDto request = result.request();
		boolean creating = state.mode() == CustomerViewState.Mode.CREATING;
		CompletableFuture<CustomerResponseDto> future = creating
				? customerService.create(request)
				: customerService.update(state.selected().orElseThrow().id(), request);

		future.whenComplete((saved, throwable) -> Platform.runLater(() -> {
			state.endOperation();
			if (throwable != null) {
				// All create/update failures (validation, duplicate conflict, not found,
				// connection) belong to the form area, never above the table.
				messages.formErrors(CustomerMessages.saveFailureLines(throwable));
				renderMessages();
				refreshControls();
			} else {
				state.cancelForm();
				if (saved != null) {
					state.select(saved);
				}
				loadCustomersData();
				messages.success(creating ? "Customer created." : "Customer updated.");
				renderMessages();
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
		messages.clearAll();
		renderMessages();
		refreshControls();

		customerService.delete(customer.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// A record-level delete conflict (rental reference) belongs above the
						// table, not in the form validation area.
						messages.statusError(CustomerMessages.forDeleteFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.clearSelection();
						loadCustomersData();
						messages.success("Customer deleted.");
						renderMessages();
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

	/**
	 * Mirrors the {@link CustomerMessageState} onto the two message areas: one
	 * wrapping label per form error below the form (never truncated), and the general
	 * status message above the table. Because the model keeps the two areas mutually
	 * exclusive, the same failure is never shown in both places.
	 */
	private void renderMessages() {
		validationMessagesContainer.getChildren().clear();
		for (String line : messages.formMessages()) {
			Label label = new Label(line);
			label.setWrapText(true);
			label.setMaxWidth(Double.MAX_VALUE);
			label.getStyleClass().add("customer-validation-message");
			validationMessagesContainer.getChildren().add(label);
		}
		setVisibleManaged(validationMessagesContainer, messages.hasFormMessages());

		statusMessageLabel.setText(messages.statusMessage());
		switch (messages.statusKind()) {
			case SUCCESS -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_SUCCESS);
			case ERROR -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_ERROR);
			case NONE -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE);
		}
		setVisibleManaged(statusMessageLabel, messages.hasStatusMessage());
	}
}

package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalRequestDto;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.service.CustomerService;
import be.condorcet.easycarrent.desktop.service.RentalFormatter;
import be.condorcet.easycarrent.desktop.service.RentalMessages;
import be.condorcet.easycarrent.desktop.service.RentalService;
import be.condorcet.easycarrent.desktop.service.RentalValidator;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the Rentals screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after loading,
 * which also triggers the first asynchronous loads. All API work runs through
 * {@link RentalService}, {@link CustomerService}, and {@link VehicleService} (the
 * last two only to populate the editor's choices); there is no direct HTTP, no
 * blocking call, and every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. Write controls are shown only when the
 * authenticated role may write, and each lifecycle action is offered only for the
 * statuses the backend permits; the backend remains authoritative. The status and
 * the total price are backend-managed and shown read-only; the editor only shows a
 * clearly-labelled, non-authoritative estimate. State is conveyed through CSS
 * classes, and no customer data beyond the name is displayed.</p>
 */
public class RentalController {

	private static final String STATUS_BASE = "rental-status-message";
	private static final String STATUS_SUCCESS = "rental-status-success";
	private static final String STATUS_ERROR = "rental-status-error";
	private static final int MONETARY_SCALE = 2;

	@FXML
	private TableView<RentalResponseDto> rentalTable;

	@FXML
	private TableColumn<RentalResponseDto, Long> idColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> customerColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> vehicleColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> startDateColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> endDateColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> statusColumn;

	@FXML
	private TableColumn<RentalResponseDto, String> totalPriceColumn;

	@FXML
	private Button refreshButton;

	@FXML
	private Button addButton;

	@FXML
	private Button editButton;

	@FXML
	private Button startRentalButton;

	@FXML
	private Button completeRentalButton;

	@FXML
	private Button cancelRentalButton;

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
	private VBox rentalEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private ComboBox<CustomerResponseDto> customerComboBox;

	@FXML
	private ComboBox<VehicleResponseDto> vehicleComboBox;

	@FXML
	private DatePicker startDatePicker;

	@FXML
	private DatePicker endDatePicker;

	@FXML
	private Label totalPricePreviewLabel;

	@FXML
	private VBox formMessagesContainer;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelEditButton;

	private RentalService rentalService;
	private CustomerService customerService;
	private VehicleService vehicleService;
	private RentalViewState state;
	private final RentalMessageState messages = new RentalMessageState();

	/** The full vehicle list, used for edit mode and the estimate; create mode may narrow it. */
	private List<VehicleResponseDto> allVehicles = List.of();

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(RentalService rentalService, CustomerService customerService,
			VehicleService vehicleService, SessionManager sessionManager) {
		this.rentalService = Objects.requireNonNull(rentalService, "rentalService");
		this.customerService = Objects.requireNonNull(customerService, "customerService");
		this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser().map(user -> user.role()).orElse(null);
		this.state = RentalViewState.forRole(role);

		configureTable();
		configureComboBoxes();
		rentalTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});
		startDatePicker.valueProperty().addListener((observable, previous, current) ->
				onEditorPeriodChanged());
		endDatePicker.valueProperty().addListener((observable, previous, current) ->
				onEditorPeriodChanged());

		refreshControls();
		renderMessages();
		loadAll();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		customerColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(customerLabel(cell.getValue())));
		vehicleColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(vehicleLabel(cell.getValue())));
		startDateColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(RentalFormatter.formatDate(cell.getValue().startDate())));
		endDateColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(RentalFormatter.formatDate(cell.getValue().endDate())));
		statusColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(RentalFormatter.formatStatus(cell.getValue().status())));
		totalPriceColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(RentalFormatter.formatPrice(cell.getValue().totalPrice())));
	}

	private void configureComboBoxes() {
		customerComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(CustomerResponseDto customer) {
				return customer == null ? "" : customerName(customer);
			}

			@Override
			public CustomerResponseDto fromString(String string) {
				return null;
			}
		});
		vehicleComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(VehicleResponseDto vehicle) {
				return vehicle == null ? "" : vehicleOptionLabel(vehicle);
			}

			@Override
			public VehicleResponseDto fromString(String string) {
				return null;
			}
		});
	}

	// --- Loading ---------------------------------------------------------------

	/** Loads the customer and vehicle lookups, then the rental list. */
	private void loadAll() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		refreshControls();

		customerService.findAll().whenComplete((customers, customerError) ->
				Platform.runLater(() -> {
					applyCustomerResult(customers, customerError);
					vehicleService.findAll().whenComplete((vehicles, vehicleError) ->
							Platform.runLater(() -> {
								applyVehicleResult(vehicles, vehicleError);
								loadRentals();
							}));
				}));
	}

	private void applyCustomerResult(List<CustomerResponseDto> customers, Throwable error) {
		if (error != null) {
			customerComboBox.setItems(FXCollections.observableArrayList());
			state.setCustomersAvailable(false);
		} else {
			customerComboBox.setItems(FXCollections.observableArrayList(customers));
			state.setCustomersAvailable(!customers.isEmpty());
		}
	}

	private void applyVehicleResult(List<VehicleResponseDto> vehicles, Throwable error) {
		if (error != null) {
			allVehicles = List.of();
			vehicleComboBox.setItems(FXCollections.observableArrayList());
			state.setVehiclesAvailable(false);
		} else {
			allVehicles = List.copyOf(vehicles);
			vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
			state.setVehiclesAvailable(!vehicles.isEmpty());
		}
	}

	/**
	 * Loads the rental list. Only touches the list and the general status area; it
	 * never clears a message the caller has just set (so a post-operation success
	 * message survives the subsequent reload).
	 */
	private void loadRentals() {
		rentalService.findAll().whenComplete((rentals, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						messages.statusError(RentalMessages.forLoadFailure(throwable));
					} else {
						state.loadSucceeded(rentals);
						renderRentals();
						maybeNoteMissingLookups();
					}
					renderMessages();
					refreshControls();
				}));
	}

	private void renderRentals() {
		rentalTable.setItems(FXCollections.observableArrayList(state.rentals()));
		state.selected().ifPresentOrElse(
				selected -> rentalTable.getSelectionModel().select(selected),
				() -> rentalTable.getSelectionModel().clearSelection());
	}

	/**
	 * When the role may write but a lookup is unavailable, explains why booking is
	 * disabled. It never overwrites an already-set message (such as a fresh success).
	 */
	private void maybeNoteMissingLookups() {
		if (state.isReadOnly() || messages.hasStatusMessage() || messages.hasFormMessages()) {
			return;
		}
		if (!state.customersAvailable() && !state.vehiclesAvailable()) {
			messages.statusError("Add at least one customer and one vehicle before booking a rental.");
		} else if (!state.customersAvailable()) {
			messages.statusError("No customers are available; add a customer before booking a rental.");
		} else if (!state.vehiclesAvailable()) {
			messages.statusError("No vehicles are available; add a vehicle before booking a rental.");
		}
	}

	@FXML
	private void handleRefresh() {
		messages.clearAll();
		renderMessages();
		loadAll();
	}

	// --- Create / edit ---------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Rental");
		clearEditorFields();
		showAllVehicles(null);
		messages.clearAll();
		renderMessages();
		refreshControls();
		customerComboBox.requestFocus();
	}

	@FXML
	private void handleEdit() {
		RentalResponseDto selected = rentalTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.beginEdit()) {
			return;
		}
		formTitleLabel.setText("Edit Rental");
		// Edit uses the full vehicle list so the currently-booked vehicle stays selectable.
		showAllVehicles(selected.vehicleId());
		selectCustomerById(selected.customerId());
		startDatePicker.setValue(selected.startDate());
		endDatePicker.setValue(selected.endDate());
		updateEstimate();
		messages.clearAll();
		renderMessages();
		refreshControls();
		customerComboBox.requestFocus();
	}

	@FXML
	private void handleCancelEdit() {
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

		Long customerId = selectedCustomerId();
		Long vehicleId = selectedVehicleId();
		RentalValidator.Result result = RentalValidator.validate(
				customerId, vehicleId, startDatePicker.getValue(), endDatePicker.getValue());
		if (!result.isValid()) {
			messages.formErrors(RentalMessages.localValidationLines(result.errors()));
			renderMessages();
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		refreshControls();

		RentalRequestDto request = result.request();
		boolean creating = state.mode() == RentalViewState.Mode.CREATING;
		CompletableFuture<RentalResponseDto> future = creating
				? rentalService.create(request)
				: rentalService.update(state.selected().orElseThrow().id(), request);

		future.whenComplete((saved, throwable) -> Platform.runLater(() -> {
			state.endOperation();
			if (throwable != null) {
				// Every create/update failure (validation, overlap or unavailable-vehicle
				// conflict, missing reference, connection) belongs below the form.
				messages.formErrors(RentalMessages.saveFailureLines(throwable));
				renderMessages();
				refreshControls();
			} else {
				state.cancelForm();
				if (saved != null) {
					state.select(saved);
				}
				loadRentals();
				messages.success(creating ? "Rental created." : "Rental updated.");
				renderMessages();
			}
		}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		RentalResponseDto selected = rentalTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete rental #" + selected.id() + " (" + vehicleLabel(selected) + ", "
						+ RentalFormatter.formatDate(selected.startDate()) + " to "
						+ RentalFormatter.formatDate(selected.endDate()) + ")? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete rental");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deleteRental(selected));
	}

	private void deleteRental(RentalResponseDto rental) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		rentalService.delete(rental.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// A record-level conflict (active/completed rental, or a payment
						// reference) belongs above the table, not in the form area.
						messages.statusError(RentalMessages.forDeleteFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.clearSelection();
						loadRentals();
						messages.success("Rental deleted.");
						renderMessages();
					}
				}));
	}

	// --- Lifecycle transitions -------------------------------------------------

	@FXML
	private void handleStart() {
		runTransition(state.canStart(), "start", "Start rental",
				selected -> "Start rental #" + selected.id() + "? The vehicle must be available.",
				id -> rentalService.start(id), "Rental started.");
	}

	@FXML
	private void handleComplete() {
		runTransition(state.canComplete(), "complete", "Complete rental",
				selected -> "Complete rental #" + selected.id()
						+ "? The vehicle will be returned to available.",
				id -> rentalService.complete(id), "Rental completed.");
	}

	@FXML
	private void handleCancelRental() {
		runTransition(state.canCancel(), "cancel", "Cancel rental",
				selected -> "Cancel rental #" + selected.id() + "? This cannot be undone.",
				id -> rentalService.cancel(id), "Rental cancelled.");
	}

	private void runTransition(boolean allowed, String action, String title,
			java.util.function.Function<RentalResponseDto, String> prompt,
			java.util.function.LongFunction<CompletableFuture<RentalResponseDto>> operation,
			String successMessage) {
		RentalResponseDto selected = rentalTable.getSelectionModel().getSelectedItem();
		if (selected == null || !allowed) {
			return;
		}
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				prompt.apply(selected), ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText(title);
		confirmation.setTitle(title);
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> applyTransition(selected, operation, successMessage));
	}

	private void applyTransition(RentalResponseDto rental,
			java.util.function.LongFunction<CompletableFuture<RentalResponseDto>> operation,
			String successMessage) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		operation.apply(rental.id()).whenComplete((updated, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// The status is only changed after backend confirmation; on a conflict
						// the row is kept and a safe message is shown above the table.
						messages.statusError(RentalMessages.forTransitionFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						if (updated != null) {
							state.select(updated);
						}
						loadRentals();
						messages.success(successMessage);
						renderMessages();
					}
				}));
	}

	// --- Editor helpers --------------------------------------------------------

	/**
	 * When both dates form a valid period in create mode, narrows the vehicle choices
	 * to those the backend reports as available for that period; otherwise leaves the
	 * full list. Always refreshes the non-authoritative estimate.
	 */
	private void onEditorPeriodChanged() {
		updateEstimate();
		if (state.mode() != RentalViewState.Mode.CREATING) {
			return;
		}
		LocalDate start = startDatePicker.getValue();
		LocalDate end = endDatePicker.getValue();
		if (start == null || end == null || !end.isAfter(start)) {
			return;
		}
		vehicleService.findAvailable(start, end).whenComplete((available, throwable) ->
				Platform.runLater(() -> {
					// Ignore a stale result if the user changed the period or left create mode.
					if (state.mode() != RentalViewState.Mode.CREATING
							|| !start.equals(startDatePicker.getValue())
							|| !end.equals(endDatePicker.getValue())) {
						return;
					}
					if (throwable == null && available != null) {
						Long previous = selectedVehicleId();
						vehicleComboBox.setItems(FXCollections.observableArrayList(available));
						reselectVehicle(previous);
					}
					// On failure keep the full list; the backend still rejects an overlap on save.
					updateEstimate();
				}));
	}

	private void updateEstimate() {
		VehicleResponseDto vehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
		LocalDate start = startDatePicker.getValue();
		LocalDate end = endDatePicker.getValue();
		if (vehicle == null || vehicle.dailyPrice() == null
				|| start == null || end == null || !end.isAfter(start)) {
			totalPricePreviewLabel.setText("—");
			return;
		}
		long days = ChronoUnit.DAYS.between(start, end);
		BigDecimal estimate = vehicle.dailyPrice()
				.multiply(BigDecimal.valueOf(days))
				.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
		totalPricePreviewLabel.setText(estimate.toPlainString() + " (estimate)");
	}

	private void showAllVehicles(Long selectId) {
		vehicleComboBox.setItems(FXCollections.observableArrayList(allVehicles));
		reselectVehicle(selectId);
	}

	private void reselectVehicle(Long vehicleId) {
		if (vehicleId == null) {
			vehicleComboBox.getSelectionModel().clearSelection();
			return;
		}
		vehicleComboBox.getItems().stream()
				.filter(vehicle -> Objects.equals(vehicle.id(), vehicleId))
				.findFirst()
				.ifPresentOrElse(
						vehicle -> vehicleComboBox.getSelectionModel().select(vehicle),
						() -> vehicleComboBox.getSelectionModel().clearSelection());
	}

	private void selectCustomerById(Long customerId) {
		customerComboBox.getItems().stream()
				.filter(customer -> Objects.equals(customer.id(), customerId))
				.findFirst()
				.ifPresentOrElse(
						customer -> customerComboBox.getSelectionModel().select(customer),
						() -> customerComboBox.getSelectionModel().clearSelection());
	}

	private void clearEditorFields() {
		customerComboBox.getSelectionModel().clearSelection();
		vehicleComboBox.getSelectionModel().clearSelection();
		startDatePicker.setValue(null);
		endDatePicker.setValue(null);
		totalPricePreviewLabel.setText("—");
	}

	private Long selectedCustomerId() {
		CustomerResponseDto customer = customerComboBox.getSelectionModel().getSelectedItem();
		return customer == null ? null : customer.id();
	}

	private Long selectedVehicleId() {
		VehicleResponseDto vehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
		return vehicle == null ? null : vehicle.id();
	}

	// --- Control state ---------------------------------------------------------

	private void refreshControls() {
		boolean readOnly = state.isReadOnly();
		setVisibleManaged(readOnlyNoticeLabel, readOnly);
		setVisibleManaged(addButton, !readOnly);
		setVisibleManaged(editButton, !readOnly);
		setVisibleManaged(startRentalButton, !readOnly);
		setVisibleManaged(completeRentalButton, !readOnly);
		setVisibleManaged(cancelRentalButton, !readOnly);
		setVisibleManaged(deleteButton, state.canEverDelete());

		addButton.setDisable(!state.canCreate());
		editButton.setDisable(!state.canEdit());
		startRentalButton.setDisable(!state.canStart());
		completeRentalButton.setDisable(!state.canComplete());
		cancelRentalButton.setDisable(!state.canCancel());
		deleteButton.setDisable(!state.canDelete());
		refreshButton.setDisable(!state.canRefresh());
		saveButton.setDisable(state.isBusy());
		cancelEditButton.setDisable(state.isBusy());

		loadingIndicator.setVisible(state.isLoading());
		setVisibleManaged(emptyStateLabel, state.isEmpty());
		setVisibleManaged(rentalEditor, state.isFormVisible());
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	// --- Display labels --------------------------------------------------------

	private String customerLabel(RentalResponseDto rental) {
		String first = rental.customerFirstName() == null ? "" : rental.customerFirstName();
		String last = rental.customerLastName() == null ? "" : rental.customerLastName();
		return (first + " " + last).trim();
	}

	private static String customerName(CustomerResponseDto customer) {
		String first = customer.firstName() == null ? "" : customer.firstName();
		String last = customer.lastName() == null ? "" : customer.lastName();
		return (first + " " + last).trim();
	}

	private String vehicleLabel(RentalResponseDto rental) {
		return joinVehicle(rental.vehicleRegistrationNumber(), rental.vehicleBrand(),
				rental.vehicleModel());
	}

	private static String vehicleOptionLabel(VehicleResponseDto vehicle) {
		return joinVehicle(vehicle.registrationNumber(), vehicle.brand(), vehicle.model());
	}

	private static String joinVehicle(String registration, String brand, String model) {
		String reg = registration == null ? "" : registration;
		String make = ((brand == null ? "" : brand) + " " + (model == null ? "" : model)).trim();
		if (reg.isEmpty()) {
			return make;
		}
		return make.isEmpty() ? reg : reg + " — " + make;
	}

	// --- Messages --------------------------------------------------------------

	/**
	 * Mirrors the {@link RentalMessageState} onto the two message areas: one wrapping
	 * label per form error below the editor (never truncated) and the general status
	 * message above the table. Because the model keeps the two areas mutually
	 * exclusive, the same failure is never shown in both places.
	 */
	private void renderMessages() {
		formMessagesContainer.getChildren().clear();
		for (String line : messages.formMessages()) {
			Label label = new Label(line);
			label.setWrapText(true);
			label.setMaxWidth(Double.MAX_VALUE);
			label.getStyleClass().add("rental-validation-message");
			formMessagesContainer.getChildren().add(label);
		}
		setVisibleManaged(formMessagesContainer, messages.hasFormMessages());

		statusMessageLabel.setText(messages.statusMessage());
		switch (messages.statusKind()) {
			case SUCCESS -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_SUCCESS);
			case ERROR -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE, STATUS_ERROR);
			case NONE -> statusMessageLabel.getStyleClass().setAll(STATUS_BASE);
		}
		setVisibleManaged(statusMessageLabel, messages.hasStatusMessage());
	}
}

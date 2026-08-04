package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.MaintenanceRequestDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;
import be.condorcet.easycarrent.desktop.service.MaintenanceFormatter;
import be.condorcet.easycarrent.desktop.service.MaintenanceMessages;
import be.condorcet.easycarrent.desktop.service.MaintenanceService;
import be.condorcet.easycarrent.desktop.service.MaintenanceValidator;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the Maintenance screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after loading,
 * which also triggers the first asynchronous loads. All API work runs through
 * {@link MaintenanceService} and {@link VehicleService} (the latter only to populate
 * the editor's vehicle choices and to enrich the table label); there is no direct
 * HTTP, no blocking call, and every UI update from an async completion is marshalled
 * with {@link Platform#runLater(Runnable)}. Every maintenance write is ADMIN-only, so
 * a USER sees a read-only screen; each lifecycle action is offered only for the
 * statuses the backend permits, and the status changes only after backend
 * confirmation. The status is backend-managed and shown read-only; the backend has
 * no maintenance update, so the screen offers no edit. State is conveyed through CSS
 * classes.</p>
 */
public class MaintenanceController {

	private static final String STATUS_BASE = "maintenance-status-message";
	private static final String STATUS_SUCCESS = "maintenance-status-success";
	private static final String STATUS_ERROR = "maintenance-status-error";

	@FXML
	private TableView<MaintenanceResponseDto> maintenanceTable;

	@FXML
	private TableColumn<MaintenanceResponseDto, Long> idColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> vehicleColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> descriptionColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> startDateColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> endDateColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> costColumn;

	@FXML
	private TableColumn<MaintenanceResponseDto, String> statusColumn;

	@FXML
	private Button refreshButton;

	@FXML
	private Button addButton;

	@FXML
	private Button startButton;

	@FXML
	private Button completeButton;

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
	private VBox maintenanceEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private ComboBox<VehicleResponseDto> vehicleComboBox;

	@FXML
	private TextArea descriptionArea;

	@FXML
	private DatePicker startDatePicker;

	@FXML
	private DatePicker endDatePicker;

	@FXML
	private TextField costField;

	@FXML
	private VBox formMessagesContainer;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelEditButton;

	private MaintenanceService maintenanceService;
	private VehicleService vehicleService;
	private MaintenanceViewState state;
	private final MaintenanceMessageState messages = new MaintenanceMessageState();

	/** All vehicles from the last lookup, indexed by id, to enrich the table label. */
	private Map<Long, VehicleResponseDto> vehiclesById = Map.of();
	private boolean vehicleLookupFailed;

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(MaintenanceService maintenanceService, VehicleService vehicleService,
			SessionManager sessionManager) {
		this.maintenanceService = Objects.requireNonNull(maintenanceService, "maintenanceService");
		this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser().map(user -> user.role()).orElse(null);
		this.state = MaintenanceViewState.forRole(role);

		configureTable();
		configureVehicleComboBox();
		maintenanceTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});

		refreshControls();
		renderMessages();
		loadAll();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		vehicleColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(recordVehicleLabel(cell.getValue())));
		descriptionColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().description()));
		startDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				MaintenanceFormatter.formatDate(cell.getValue().startDate())));
		endDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				MaintenanceFormatter.formatDate(cell.getValue().endDate())));
		costColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				MaintenanceFormatter.formatCost(cell.getValue().cost())));
		statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				MaintenanceFormatter.formatStatus(cell.getValue().status())));
	}

	private void configureVehicleComboBox() {
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

	/** Loads the vehicle lookup, then the maintenance list. */
	private void loadAll() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		refreshControls();

		vehicleService.findAll().whenComplete((vehicles, vehicleError) ->
				Platform.runLater(() -> {
					applyVehicleResult(vehicles, vehicleError);
					loadRecords();
				}));
	}

	private void applyVehicleResult(List<VehicleResponseDto> vehicles, Throwable error) {
		if (error != null) {
			vehicleLookupFailed = true;
			vehiclesById = Map.of();
			vehicleComboBox.setItems(FXCollections.observableArrayList());
			state.setVehiclesAvailable(false);
			return;
		}
		vehicleLookupFailed = false;
		Map<Long, VehicleResponseDto> index = new LinkedHashMap<>();
		for (VehicleResponseDto vehicle : vehicles) {
			if (vehicle.id() != null) {
				index.put(vehicle.id(), vehicle);
			}
		}
		vehiclesById = index;
		// Only non-inactive vehicles may have maintenance scheduled (the backend rejects
		// INACTIVE); temporal conflicts remain the backend's authority.
		List<VehicleResponseDto> maintainable = vehicles.stream()
				.filter(vehicle -> vehicle.id() != null)
				.filter(vehicle -> vehicle.status() != VehicleStatus.INACTIVE)
				.toList();
		Long previous = selectedVehicleId();
		vehicleComboBox.setItems(FXCollections.observableArrayList(maintainable));
		reselectVehicle(previous);
		state.setVehiclesAvailable(!maintainable.isEmpty());
	}

	/**
	 * Loads the maintenance list. Only touches the list and the general status area on
	 * failure; it never clears a message the caller has just set (so a post-operation
	 * success message survives the subsequent reload).
	 */
	private void loadRecords() {
		maintenanceService.findAll().whenComplete((records, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						messages.statusError(MaintenanceMessages.forLoadFailure(throwable));
					} else {
						state.loadSucceeded(records);
						renderRecords();
						maybeNoteNoMaintainableVehicle();
					}
					renderMessages();
					refreshControls();
				}));
	}

	private void renderRecords() {
		maintenanceTable.setItems(FXCollections.observableArrayList(state.records()));
		state.selected().ifPresentOrElse(
				selected -> maintenanceTable.getSelectionModel().select(selected),
				() -> maintenanceTable.getSelectionModel().clearSelection());
	}

	/**
	 * When the role may write but no maintainable vehicle is available, explains why
	 * scheduling is disabled. It never overwrites an already-set message.
	 */
	private void maybeNoteNoMaintainableVehicle() {
		if (state.isReadOnly() || messages.hasStatusMessage() || messages.hasFormMessages()
				|| state.vehiclesAvailable()) {
			return;
		}
		if (vehicleLookupFailed) {
			messages.statusError("Vehicles could not be loaded; scheduling maintenance is disabled.");
		} else {
			messages.statusError("No active vehicle is available to schedule maintenance for.");
		}
	}

	@FXML
	private void handleRefresh() {
		messages.clearAll();
		renderMessages();
		loadAll();
	}

	// --- Create ----------------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Maintenance");
		clearEditorFields();
		messages.clearAll();
		renderMessages();
		refreshControls();
		vehicleComboBox.requestFocus();
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

		MaintenanceValidator.Result result = MaintenanceValidator.validate(
				selectedVehicleId(), descriptionArea.getText(), startDatePicker.getValue(),
				endDatePicker.getValue(), costField.getText());
		if (!result.isValid()) {
			messages.formErrors(MaintenanceMessages.localValidationLines(result.errors()));
			renderMessages();
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		refreshControls();

		MaintenanceRequestDto request = result.request();
		maintenanceService.create(request).whenComplete((saved, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// Every create failure (validation, overlap, inactive vehicle, missing
						// vehicle, connection) belongs below the form.
						messages.formErrors(MaintenanceMessages.saveFailureLines(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.cancelForm();
						if (saved != null) {
							state.select(saved);
						}
						loadRecords();
						messages.success("Maintenance record created.");
						renderMessages();
					}
				}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		MaintenanceResponseDto selected = maintenanceTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete maintenance record #" + selected.id() + " for "
						+ recordVehicleLabel(selected) + " (" + MaintenanceFormatter.formatDate(
								selected.startDate()) + " to " + MaintenanceFormatter.formatDate(
								selected.endDate()) + ")? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete maintenance record");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deleteRecord(selected));
	}

	private void deleteRecord(MaintenanceResponseDto record) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		maintenanceService.delete(record.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// A record-level conflict (an in-progress or completed record) belongs
						// above the table, not in the form area.
						messages.statusError(MaintenanceMessages.forDeleteFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.clearSelection();
						loadRecords();
						messages.success("Maintenance record deleted.");
						renderMessages();
					}
				}));
	}

	// --- Lifecycle transitions -------------------------------------------------

	@FXML
	private void handleStart() {
		runTransition(state.canStart(), "Start maintenance",
				selected -> "Start maintenance record #" + selected.id()
						+ "? The vehicle must be available and will move to maintenance.",
				id -> maintenanceService.start(id), "Maintenance started.");
	}

	@FXML
	private void handleComplete() {
		runTransition(state.canComplete(), "Complete maintenance",
				selected -> "Complete maintenance record #" + selected.id()
						+ "? The vehicle will return to available.",
				id -> maintenanceService.complete(id), "Maintenance completed.");
	}

	private void runTransition(boolean allowed, String title,
			java.util.function.Function<MaintenanceResponseDto, String> prompt,
			java.util.function.LongFunction<CompletableFuture<MaintenanceResponseDto>> operation,
			String successMessage) {
		MaintenanceResponseDto selected = maintenanceTable.getSelectionModel().getSelectedItem();
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

	private void applyTransition(MaintenanceResponseDto record,
			java.util.function.LongFunction<CompletableFuture<MaintenanceResponseDto>> operation,
			String successMessage) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		operation.apply(record.id()).whenComplete((updated, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// The status is only changed after backend confirmation; on a conflict
						// the row is kept and a safe message is shown above the table.
						messages.statusError(MaintenanceMessages.forTransitionFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						if (updated != null) {
							state.select(updated);
						}
						loadRecords();
						messages.success(successMessage);
						renderMessages();
					}
				}));
	}

	// --- Editor helpers --------------------------------------------------------

	private void clearEditorFields() {
		vehicleComboBox.getSelectionModel().clearSelection();
		descriptionArea.clear();
		startDatePicker.setValue(null);
		endDatePicker.setValue(null);
		costField.clear();
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

	private Long selectedVehicleId() {
		VehicleResponseDto vehicle = vehicleComboBox.getSelectionModel().getSelectedItem();
		return vehicle == null ? null : vehicle.id();
	}

	// --- Control state ---------------------------------------------------------

	private void refreshControls() {
		boolean readOnly = state.isReadOnly();
		setVisibleManaged(readOnlyNoticeLabel, readOnly);
		setVisibleManaged(addButton, !readOnly);
		setVisibleManaged(startButton, !readOnly);
		setVisibleManaged(completeButton, !readOnly);
		setVisibleManaged(deleteButton, !readOnly);

		addButton.setDisable(!state.canCreate());
		startButton.setDisable(!state.canStart());
		completeButton.setDisable(!state.canComplete());
		deleteButton.setDisable(!state.canDelete());
		refreshButton.setDisable(!state.canRefresh());
		saveButton.setDisable(state.isBusy());
		cancelEditButton.setDisable(state.isBusy());

		loadingIndicator.setVisible(state.isLoading());
		setVisibleManaged(emptyStateLabel, state.isEmpty());
		setVisibleManaged(maintenanceEditor, state.isFormVisible());
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	// --- Display labels --------------------------------------------------------

	private String recordVehicleLabel(MaintenanceResponseDto record) {
		VehicleResponseDto vehicle = record.vehicleId() == null ? null
				: vehiclesById.get(record.vehicleId());
		if (vehicle != null) {
			return vehicleOptionLabel(vehicle);
		}
		return "Vehicle #" + record.vehicleId();
	}

	private static String vehicleOptionLabel(VehicleResponseDto vehicle) {
		String reg = vehicle.registrationNumber() == null ? "" : vehicle.registrationNumber();
		String make = ((vehicle.brand() == null ? "" : vehicle.brand()) + " "
				+ (vehicle.model() == null ? "" : vehicle.model())).trim();
		if (reg.isEmpty()) {
			return make;
		}
		return make.isEmpty() ? reg : reg + " — " + make;
	}

	// --- Messages --------------------------------------------------------------

	/**
	 * Mirrors the {@link MaintenanceMessageState} onto the two message areas: one
	 * wrapping label per form error below the editor (never truncated) and the general
	 * status message above the table. Because the model keeps the two areas mutually
	 * exclusive, the same failure is never shown in both places.
	 */
	private void renderMessages() {
		formMessagesContainer.getChildren().clear();
		for (String line : messages.formMessages()) {
			Label label = new Label(line);
			label.setWrapText(true);
			label.setMaxWidth(Double.MAX_VALUE);
			label.getStyleClass().add("maintenance-validation-message");
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

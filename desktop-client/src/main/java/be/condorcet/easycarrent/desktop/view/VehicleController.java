package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.auth.VehiclePermissions;
import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleService;
import be.condorcet.easycarrent.desktop.service.VehicleValidator;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.time.Year;
import java.util.List;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the Vehicles screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after
 * loading, which also triggers the first asynchronous loads. All API work runs
 * through {@link VehicleService} and {@link VehicleCategoryService} (the latter
 * only to populate the editor's category choices); there is no direct HTTP, no
 * blocking call, and every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. Write controls are shown only when the
 * authenticated role may write; the backend remains authoritative. The status is
 * backend-managed and shown read-only. State is conveyed through CSS classes.</p>
 */
public class VehicleController {

	private static final String STATUS_BASE = "vehicle-status";
	private static final String STATUS_SUCCESS = "vehicle-status-success";
	private static final String STATUS_ERROR = "vehicle-status-error";

	@FXML
	private TableView<VehicleResponseDto> vehicleTable;

	@FXML
	private TableColumn<VehicleResponseDto, Long> idColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> registrationColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> brandColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> modelColumn;

	@FXML
	private TableColumn<VehicleResponseDto, Integer> yearColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> categoryColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> statusColumn;

	@FXML
	private TableColumn<VehicleResponseDto, String> dailyRateColumn;

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
	private VBox vehicleEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private TextField registrationField;

	@FXML
	private TextField brandField;

	@FXML
	private TextField modelField;

	@FXML
	private TextField manufacturingYearField;

	@FXML
	private TextField colorField;

	@FXML
	private TextField dailyRateField;

	@FXML
	private TextField mileageField;

	@FXML
	private ComboBox<VehicleCategoryResponseDto> categoryComboBox;

	@FXML
	private Label validationMessageLabel;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelButton;

	private VehicleService vehicleService;
	private VehicleCategoryService categoryService;
	private VehicleViewState state;

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(VehicleService vehicleService, VehicleCategoryService categoryService,
			SessionManager sessionManager) {
		this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
		this.categoryService = Objects.requireNonNull(categoryService, "categoryService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser().map(user -> user.role()).orElse(null);
		this.state = new VehicleViewState(VehiclePermissions.canWrite(role));

		configureTable();
		configureCategoryComboBox();
		vehicleTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});

		refreshControls();
		loadAll();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		registrationColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().registrationNumber()));
		brandColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().brand()));
		modelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().model()));
		yearColumn.setCellValueFactory(
				cell -> new SimpleObjectProperty<>(cell.getValue().manufacturingYear()));
		categoryColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().categoryName()));
		statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				cell.getValue().status() == null ? "" : cell.getValue().status().displayLabel()));
		dailyRateColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(formatPrice(cell.getValue().dailyPrice())));
	}

	private void configureCategoryComboBox() {
		categoryComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(VehicleCategoryResponseDto category) {
				return category == null ? "" : category.name();
			}

			@Override
			public VehicleCategoryResponseDto fromString(String string) {
				return null;
			}
		});
	}

	// --- Loading ---------------------------------------------------------------

	private void loadAll() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		clearStatus();
		refreshControls();

		categoryService.findAll().whenComplete((categories, categoryError) ->
				Platform.runLater(() -> {
					applyCategoryResult(categories, categoryError);
					loadVehicles(categoryError != null);
				}));
	}

	private void applyCategoryResult(List<VehicleCategoryResponseDto> categories, Throwable error) {
		if (error != null) {
			categoryComboBox.setItems(FXCollections.observableArrayList());
			state.setCategoriesAvailable(false);
		} else {
			categoryComboBox.setItems(FXCollections.observableArrayList(categories));
			state.setCategoriesAvailable(!categories.isEmpty());
		}
	}

	private void loadVehicles(boolean categoriesFailed) {
		vehicleService.findAll().whenComplete((vehicles, error) ->
				Platform.runLater(() -> {
					if (error != null) {
						state.loadFailed();
						showStatusError(safeMessage(error));
					} else {
						state.loadSucceeded(vehicles);
						renderVehicles();
						showCategoryAvailabilityNote(categoriesFailed);
					}
					refreshControls();
				}));
	}

	private void reloadVehicles() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		refreshControls();
		vehicleService.findAll().whenComplete((vehicles, error) ->
				Platform.runLater(() -> {
					if (error != null) {
						state.loadFailed();
						showStatusError(safeMessage(error));
					} else {
						state.loadSucceeded(vehicles);
						renderVehicles();
					}
					refreshControls();
				}));
	}

	private void renderVehicles() {
		vehicleTable.setItems(FXCollections.observableArrayList(state.vehicles()));
		state.selected().ifPresentOrElse(
				selected -> vehicleTable.getSelectionModel().select(selected),
				() -> vehicleTable.getSelectionModel().clearSelection());
	}

	private void showCategoryAvailabilityNote(boolean categoriesFailed) {
		if (!state.isReadOnly() && !state.categoriesAvailable()) {
			showStatusError(categoriesFailed
					? "Categories could not be loaded; creating and editing vehicles is disabled."
					: "No categories exist yet; create a category before adding vehicles.");
		}
	}

	@FXML
	private void handleRefresh() {
		loadAll();
	}

	// --- Create / edit ---------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Vehicle");
		clearFormFields();
		categoryComboBox.getSelectionModel().clearSelection();
		clearValidation();
		clearStatus();
		refreshControls();
		registrationField.requestFocus();
	}

	@FXML
	private void handleEdit() {
		VehicleResponseDto selected = vehicleTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.beginEdit()) {
			return;
		}
		formTitleLabel.setText("Edit Vehicle");
		registrationField.setText(selected.registrationNumber());
		brandField.setText(selected.brand());
		modelField.setText(selected.model());
		manufacturingYearField.setText(
				selected.manufacturingYear() == null ? "" : selected.manufacturingYear().toString());
		colorField.setText(selected.color() == null ? "" : selected.color());
		dailyRateField.setText(
				selected.dailyPrice() == null ? "" : selected.dailyPrice().toPlainString());
		mileageField.setText(selected.mileage() == null ? "" : selected.mileage().toString());
		selectCategoryById(selected.categoryId());
		clearStatus();
		refreshControls();
		registrationField.requestFocus();
	}

	private void selectCategoryById(Long categoryId) {
		VehicleCategoryResponseDto match = categoryComboBox.getItems().stream()
				.filter(category -> Objects.equals(category.id(), categoryId))
				.findFirst()
				.orElse(null);
		if (match != null) {
			categoryComboBox.getSelectionModel().select(match);
			clearValidation();
		} else {
			categoryComboBox.getSelectionModel().clearSelection();
			showValidation("The vehicle's category is no longer available; choose a category.");
		}
	}

	@FXML
	private void handleCancel() {
		state.cancelForm();
		clearValidation();
		refreshControls();
	}

	@FXML
	private void handleSave() {
		VehicleCategoryResponseDto category = categoryComboBox.getSelectionModel().getSelectedItem();
		Long categoryId = category == null ? null : category.id();

		VehicleValidator.Result result = VehicleValidator.validate(
				registrationField.getText(), brandField.getText(), modelField.getText(),
				manufacturingYearField.getText(), colorField.getText(), dailyRateField.getText(),
				mileageField.getText(), categoryId, Year.now().getValue());
		if (!result.isValid()) {
			showValidation(String.join("\n", result.errors()));
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		clearValidation();
		refreshControls();

		VehicleRequestDto request = result.request();
		boolean creating = state.mode() == VehicleViewState.Mode.CREATING;
		CompletableFuture<VehicleResponseDto> future = creating
				? vehicleService.create(request)
				: vehicleService.update(state.selected().orElseThrow().id(), request);

		future.whenComplete((saved, throwable) -> Platform.runLater(() -> {
			state.endOperation();
			if (throwable != null) {
				showValidation(safeMessage(throwable));
				refreshControls();
			} else {
				state.cancelForm();
				clearValidation();
				showStatusSuccess(creating ? "Vehicle created." : "Vehicle updated.");
				if (saved != null) {
					state.select(saved);
				}
				reloadVehicles();
			}
		}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		VehicleResponseDto selected = vehicleTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete vehicle \"" + selected.registrationNumber() + " ("
						+ selected.brand() + " " + selected.model() + ")\"? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete vehicle");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deleteVehicle(selected));
	}

	private void deleteVehicle(VehicleResponseDto vehicle) {
		if (!state.beginOperation()) {
			return;
		}
		clearStatus();
		refreshControls();

		vehicleService.delete(vehicle.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						showStatusError(safeMessage(throwable));
						refreshControls();
					} else {
						state.clearSelection();
						showStatusSuccess("Vehicle deleted.");
						reloadVehicles();
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
		setVisibleManaged(vehicleEditor, state.isFormVisible());
	}

	private void clearFormFields() {
		registrationField.clear();
		brandField.clear();
		modelField.clear();
		manufacturingYearField.clear();
		colorField.clear();
		dailyRateField.clear();
		mileageField.clear();
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	private static String formatPrice(BigDecimal price) {
		return price == null ? "" : price.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
		if (status == HttpURLConnection.HTTP_NOT_FOUND
				|| status == HttpURLConnection.HTTP_CONFLICT) {
			return request.apiError()
					.map(ApiErrorDto::message)
					.filter(message -> message != null && !message.isBlank())
					.orElse(status == HttpURLConnection.HTTP_NOT_FOUND
							? "The vehicle or category no longer exists. Please refresh."
							: "The request conflicts with existing data.");
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

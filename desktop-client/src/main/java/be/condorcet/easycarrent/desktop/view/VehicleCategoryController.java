package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.auth.VehicleCategoryPermissions;
import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryRequestDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;
import be.condorcet.easycarrent.desktop.http.ApiRequestException;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryService;
import be.condorcet.easycarrent.desktop.service.VehicleCategoryValidator;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller for the Vehicle Categories screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after
 * loading, which also triggers the first asynchronous load. All API work runs
 * through {@link VehicleCategoryService}; there is no direct HTTP, no blocking
 * call, and every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. Write controls are shown only when the
 * authenticated role may write; the backend remains authoritative. State is
 * conveyed through CSS classes, never inline styles.</p>
 */
public class VehicleCategoryController {

	private static final String STATUS_BASE = "category-status";
	private static final String STATUS_SUCCESS = "category-status-success";
	private static final String STATUS_ERROR = "category-status-error";

	@FXML
	private TableView<VehicleCategoryResponseDto> categoryTable;

	@FXML
	private TableColumn<VehicleCategoryResponseDto, Long> idColumn;

	@FXML
	private TableColumn<VehicleCategoryResponseDto, String> nameColumn;

	@FXML
	private TableColumn<VehicleCategoryResponseDto, String> descriptionColumn;

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
	private Label readOnlyNotice;

	@FXML
	private VBox categoryEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private TextField categoryNameField;

	@FXML
	private TextArea categoryDescriptionArea;

	@FXML
	private Label validationMessageLabel;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelButton;

	private VehicleCategoryService categoryService;
	private VehicleCategoryViewState state;

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(VehicleCategoryService categoryService, SessionManager sessionManager) {
		this.categoryService = Objects.requireNonNull(categoryService, "categoryService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser()
				.map(user -> user.role())
				.orElse(null);
		this.state = new VehicleCategoryViewState(VehicleCategoryPermissions.canWrite(role));

		configureTable();
		categoryTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});

		refreshControls();
		loadCategories();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
		descriptionColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(cell.getValue().description()));
	}

	// --- Loading ---------------------------------------------------------------

	private void loadCategories() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		clearStatus();
		refreshControls();

		categoryService.findAll().whenComplete((categories, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						showStatusError(safeMessage(throwable));
					} else {
						state.loadSucceeded(categories);
						renderCategories();
					}
					refreshControls();
				}));
	}

	private void renderCategories() {
		categoryTable.setItems(FXCollections.observableArrayList(state.categories()));
		state.selected().ifPresentOrElse(
				selected -> categoryTable.getSelectionModel().select(selected),
				() -> categoryTable.getSelectionModel().clearSelection());
	}

	@FXML
	private void handleRefresh() {
		loadCategories();
	}

	// --- Create / edit ---------------------------------------------------------

	@FXML
	private void handleAdd() {
		if (!state.beginCreate()) {
			return;
		}
		formTitleLabel.setText("Add Category");
		categoryNameField.clear();
		categoryDescriptionArea.clear();
		clearValidation();
		clearStatus();
		refreshControls();
		categoryNameField.requestFocus();
	}

	@FXML
	private void handleEdit() {
		VehicleCategoryResponseDto selected = categoryTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.beginEdit()) {
			return;
		}
		formTitleLabel.setText("Edit Category");
		categoryNameField.setText(selected.name());
		categoryDescriptionArea.setText(selected.description() == null ? "" : selected.description());
		clearValidation();
		clearStatus();
		refreshControls();
		categoryNameField.requestFocus();
	}

	@FXML
	private void handleCancel() {
		state.cancelForm();
		clearValidation();
		refreshControls();
	}

	@FXML
	private void handleSave() {
		String name = categoryNameField.getText();
		String description = categoryDescriptionArea.getText();

		List<String> errors = VehicleCategoryValidator.validate(name, description);
		if (!errors.isEmpty()) {
			showValidation(String.join("\n", errors));
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		clearValidation();
		refreshControls();

		VehicleCategoryRequestDto request = new VehicleCategoryRequestDto(
				name.trim(), normalizeDescription(description));
		boolean creating = state.mode() == VehicleCategoryViewState.Mode.CREATING;

		var future = creating
				? categoryService.create(request)
				: categoryService.update(state.selected().orElseThrow().id(), request);

		future.whenComplete((saved, throwable) -> Platform.runLater(() -> {
			state.endOperation();
			if (throwable != null) {
				showValidation(safeMessage(throwable));
				refreshControls();
			} else {
				state.cancelForm();
				clearValidation();
				showStatusSuccess(creating
						? "Category created."
						: "Category updated.");
				reloadAndSelect(saved);
			}
		}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		VehicleCategoryResponseDto selected = categoryTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}

		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete category \"" + selected.name() + "\"? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete category");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deleteCategory(selected));
	}

	private void deleteCategory(VehicleCategoryResponseDto category) {
		if (!state.beginOperation()) {
			return;
		}
		clearStatus();
		refreshControls();

		categoryService.delete(category.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						showStatusError(safeMessage(throwable));
						refreshControls();
					} else {
						state.clearSelection();
						showStatusSuccess("Category deleted.");
						loadCategories();
					}
				}));
	}

	private void reloadAndSelect(VehicleCategoryResponseDto saved) {
		if (saved != null) {
			state.select(saved);
		}
		loadCategories();
	}

	// --- Control state ---------------------------------------------------------

	private void refreshControls() {
		boolean readOnly = state.isReadOnly();
		setVisibleManaged(readOnlyNotice, readOnly);
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
		setVisibleManaged(categoryEditor, state.isFormVisible());
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

	private static String normalizeDescription(String description) {
		if (description == null) {
			return null;
		}
		String trimmed = description.trim();
		return trimmed.isEmpty() ? null : trimmed;
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
			return "The category no longer exists. Please refresh.";
		}
		if (status == HttpURLConnection.HTTP_CONFLICT) {
			return request.apiError()
					.map(ApiErrorDto::message)
					.filter(message -> message != null && !message.isBlank())
					.orElse("The category conflicts with existing data.");
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

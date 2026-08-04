package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.auth.DesktopUserRole;
import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentRequestDto;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
import be.condorcet.easycarrent.desktop.service.PaymentFormatter;
import be.condorcet.easycarrent.desktop.service.PaymentMessages;
import be.condorcet.easycarrent.desktop.service.PaymentService;
import be.condorcet.easycarrent.desktop.service.PaymentValidator;
import be.condorcet.easycarrent.desktop.service.RentalFormatter;
import be.condorcet.easycarrent.desktop.service.RentalService;
import be.condorcet.easycarrent.desktop.session.SessionManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the Payments screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborators are supplied through {@link #init} after loading,
 * which also triggers the first asynchronous loads. All API work runs through
 * {@link PaymentService} and {@link RentalService} (the latter only to populate the
 * editor's rental choices and to enrich the table label); there is no direct HTTP,
 * no blocking call, and every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. Write controls are shown only when the
 * authenticated role may write, and each lifecycle action is offered only for the
 * statuses the backend permits; the backend remains authoritative. The amount,
 * status, and timestamps are backend-managed and shown read-only; the editor only
 * shows the selected rental's total as the amount that will be charged. State is
 * conveyed through CSS classes, and no customer data beyond the name is displayed.
 * The backend has no payment update, so the screen offers no edit.</p>
 */
public class PaymentController {

	private static final String STATUS_BASE = "payment-status-message";
	private static final String STATUS_SUCCESS = "payment-status-success";
	private static final String STATUS_ERROR = "payment-status-error";

	/** Rental statuses for which the backend allows creating a payment. */
	private static final Set<RentalStatus> PAYABLE_RENTAL_STATUSES =
			Set.of(RentalStatus.ACTIVE, RentalStatus.COMPLETED);

	@FXML
	private TableView<PaymentResponseDto> paymentTable;

	@FXML
	private TableColumn<PaymentResponseDto, Long> idColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> rentalColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> amountColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> methodColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> statusColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> createdAtColumn;

	@FXML
	private TableColumn<PaymentResponseDto, String> paidAtColumn;

	@FXML
	private Button refreshButton;

	@FXML
	private Button addButton;

	@FXML
	private Button markPaidButton;

	@FXML
	private Button markFailedButton;

	@FXML
	private Button retryButton;

	@FXML
	private Button refundButton;

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
	private VBox paymentEditor;

	@FXML
	private Label formTitleLabel;

	@FXML
	private ComboBox<RentalResponseDto> rentalComboBox;

	@FXML
	private ComboBox<PaymentMethod> paymentMethodComboBox;

	@FXML
	private Label amountPreviewLabel;

	@FXML
	private VBox formMessagesContainer;

	@FXML
	private Button saveButton;

	@FXML
	private Button cancelEditButton;

	private PaymentService paymentService;
	private RentalService rentalService;
	private PaymentViewState state;
	private final PaymentMessageState messages = new PaymentMessageState();

	/** All rentals from the last lookup, indexed by id, to enrich the table label. */
	private Map<Long, RentalResponseDto> rentalsById = Map.of();
	private List<RentalResponseDto> allRentals = List.of();
	private boolean rentalLookupFailed;

	/** Supplies collaborators after {@code FXMLLoader.load()} and starts loading. */
	public void init(PaymentService paymentService, RentalService rentalService,
			SessionManager sessionManager) {
		this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
		this.rentalService = Objects.requireNonNull(rentalService, "rentalService");
		Objects.requireNonNull(sessionManager, "sessionManager");

		DesktopUserRole role = sessionManager.currentUser().map(user -> user.role()).orElse(null);
		this.state = PaymentViewState.forRole(role);

		configureTable();
		configureComboBoxes();
		paymentTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> {
					state.select(current);
					refreshControls();
				});
		rentalComboBox.getSelectionModel().selectedItemProperty()
				.addListener((observable, previous, current) -> updateAmountPreview());

		refreshControls();
		renderMessages();
		loadAll();
	}

	private void configureTable() {
		idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().id()));
		rentalColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(paymentRentalLabel(cell.getValue())));
		amountColumn.setCellValueFactory(
				cell -> new SimpleStringProperty(PaymentFormatter.formatAmount(cell.getValue().amount())));
		methodColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				PaymentFormatter.formatMethod(cell.getValue().paymentMethod())));
		statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				PaymentFormatter.formatStatus(cell.getValue().status())));
		createdAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				PaymentFormatter.formatDateTime(cell.getValue().createdAt())));
		paidAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(
				PaymentFormatter.formatDateTime(cell.getValue().paidAt())));
	}

	private void configureComboBoxes() {
		rentalComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(RentalResponseDto rental) {
				return rental == null ? "" : rentalOptionLabel(rental);
			}

			@Override
			public RentalResponseDto fromString(String string) {
				return null;
			}
		});
		paymentMethodComboBox.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
		paymentMethodComboBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(PaymentMethod method) {
				return method == null ? "" : method.displayLabel();
			}

			@Override
			public PaymentMethod fromString(String string) {
				return null;
			}
		});
	}

	// --- Loading ---------------------------------------------------------------

	/** Loads the rental lookup, then the payment list. */
	private void loadAll() {
		if (state.isLoading()) {
			return;
		}
		state.beginLoading();
		refreshControls();

		rentalService.findAll().whenComplete((rentals, rentalError) ->
				Platform.runLater(() -> {
					applyRentalResult(rentals, rentalError);
					loadPayments();
				}));
	}

	private void applyRentalResult(List<RentalResponseDto> rentals, Throwable error) {
		if (error != null) {
			rentalLookupFailed = true;
			allRentals = List.of();
			rentalsById = Map.of();
		} else {
			rentalLookupFailed = false;
			allRentals = List.copyOf(rentals);
			Map<Long, RentalResponseDto> index = new LinkedHashMap<>();
			for (RentalResponseDto rental : rentals) {
				if (rental.id() != null) {
					index.put(rental.id(), rental);
				}
			}
			rentalsById = index;
		}
	}

	/**
	 * Loads the payment list and recomputes the rentals eligible for a new payment.
	 * Only touches the list and the general status area on failure; it never clears a
	 * message the caller has just set (so a post-operation success survives the
	 * subsequent reload).
	 */
	private void loadPayments() {
		paymentService.findAll().whenComplete((payments, throwable) ->
				Platform.runLater(() -> {
					if (throwable != null) {
						state.loadFailed();
						messages.statusError(PaymentMessages.forLoadFailure(throwable));
					} else {
						state.loadSucceeded(payments);
						renderPayments();
						recomputeEligibleRentals(payments);
						maybeNoteNoEligibleRentals();
					}
					renderMessages();
					refreshControls();
				}));
	}

	private void renderPayments() {
		paymentTable.setItems(FXCollections.observableArrayList(state.payments()));
		state.selected().ifPresentOrElse(
				selected -> paymentTable.getSelectionModel().select(selected),
				() -> paymentTable.getSelectionModel().clearSelection());
	}

	/**
	 * A rental is eligible for a new payment when it is ACTIVE or COMPLETED and does
	 * not already have a payment. The backend remains authoritative and still rejects
	 * a duplicate or a non-payable rental.
	 */
	private void recomputeEligibleRentals(List<PaymentResponseDto> payments) {
		Set<Long> paidRentalIds = payments.stream()
				.map(PaymentResponseDto::rentalId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		List<RentalResponseDto> eligible = allRentals.stream()
				.filter(rental -> rental.id() != null)
				.filter(rental -> PAYABLE_RENTAL_STATUSES.contains(rental.status()))
				.filter(rental -> !paidRentalIds.contains(rental.id()))
				.toList();
		Long previousId = selectedRentalId();
		rentalComboBox.setItems(FXCollections.observableArrayList(eligible));
		reselectRental(previousId);
		state.setRentalsAvailable(!eligible.isEmpty());
	}

	/**
	 * When the role may write but no eligible rental is available, explains why
	 * booking a payment is disabled. It never overwrites an already-set message.
	 */
	private void maybeNoteNoEligibleRentals() {
		if (state.isReadOnly() || messages.hasStatusMessage() || messages.hasFormMessages()
				|| state.rentalsAvailable()) {
			return;
		}
		if (rentalLookupFailed) {
			messages.statusError("Rentals could not be loaded; creating a payment is disabled.");
		} else {
			messages.statusError("No active or completed rental without a payment is available.");
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
		formTitleLabel.setText("Add Payment");
		rentalComboBox.getSelectionModel().clearSelection();
		paymentMethodComboBox.getSelectionModel().clearSelection();
		updateAmountPreview();
		messages.clearAll();
		renderMessages();
		refreshControls();
		rentalComboBox.requestFocus();
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

		Long rentalId = selectedRentalId();
		PaymentMethod method = paymentMethodComboBox.getSelectionModel().getSelectedItem();
		PaymentValidator.Result result = PaymentValidator.validate(rentalId, method);
		if (!result.isValid()) {
			messages.formErrors(PaymentMessages.localValidationLines(result.errors()));
			renderMessages();
			return;
		}
		if (!state.beginOperation()) {
			return;
		}
		refreshControls();

		PaymentRequestDto request = result.request();
		paymentService.create(request).whenComplete((saved, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// Every create failure (validation, duplicate payment, non-payable rental,
						// missing rental, connection) belongs below the form.
						messages.formErrors(PaymentMessages.saveFailureLines(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.cancelForm();
						if (saved != null) {
							state.select(saved);
						}
						loadPayments();
						messages.success("Payment created.");
						renderMessages();
					}
				}));
	}

	// --- Delete ----------------------------------------------------------------

	@FXML
	private void handleDelete() {
		PaymentResponseDto selected = paymentTable.getSelectionModel().getSelectedItem();
		if (selected == null || !state.canDelete()) {
			return;
		}
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete payment #" + selected.id() + " for rental #" + selected.rentalId() + " ("
						+ PaymentFormatter.formatAmount(selected.amount()) + ")? This cannot be undone.",
				ButtonType.OK, ButtonType.CANCEL);
		confirmation.setHeaderText("Delete payment");
		confirmation.setTitle("Confirm deletion");
		confirmation.showAndWait()
				.filter(button -> button == ButtonType.OK)
				.ifPresent(button -> deletePayment(selected));
	}

	private void deletePayment(PaymentResponseDto payment) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		paymentService.delete(payment.id()).whenComplete((ignored, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// A record-level conflict (a paid or refunded payment) belongs above the
						// table, not in the form area.
						messages.statusError(PaymentMessages.forDeleteFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						state.clearSelection();
						loadPayments();
						messages.success("Payment deleted.");
						renderMessages();
					}
				}));
	}

	// --- Lifecycle transitions -------------------------------------------------

	@FXML
	private void handleMarkPaid() {
		runTransition(state.canMarkPaid(), "Mark payment paid",
				selected -> "Mark payment #" + selected.id() + " as paid?",
				id -> paymentService.markPaid(id), "Payment marked as paid.");
	}

	@FXML
	private void handleMarkFailed() {
		runTransition(state.canMarkFailed(), "Mark payment failed",
				selected -> "Mark payment #" + selected.id() + " as failed?",
				id -> paymentService.markFailed(id), "Payment marked as failed.");
	}

	@FXML
	private void handleRetry() {
		runTransition(state.canRetry(), "Retry payment",
				selected -> "Retry payment #" + selected.id() + "? It returns to pending.",
				id -> paymentService.retry(id), "Payment retried.");
	}

	@FXML
	private void handleRefund() {
		runTransition(state.canRefund(), "Refund payment",
				selected -> "Refund payment #" + selected.id() + "? This cannot be undone.",
				id -> paymentService.refund(id), "Payment refunded.");
	}

	private void runTransition(boolean allowed, String title,
			java.util.function.Function<PaymentResponseDto, String> prompt,
			java.util.function.LongFunction<CompletableFuture<PaymentResponseDto>> operation,
			String successMessage) {
		PaymentResponseDto selected = paymentTable.getSelectionModel().getSelectedItem();
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

	private void applyTransition(PaymentResponseDto payment,
			java.util.function.LongFunction<CompletableFuture<PaymentResponseDto>> operation,
			String successMessage) {
		if (!state.beginOperation()) {
			return;
		}
		messages.clearAll();
		renderMessages();
		refreshControls();

		operation.apply(payment.id()).whenComplete((updated, throwable) ->
				Platform.runLater(() -> {
					state.endOperation();
					if (throwable != null) {
						// The status is only changed after backend confirmation; on a conflict
						// the row is kept and a safe message is shown above the table.
						messages.statusError(PaymentMessages.forTransitionFailure(throwable));
						renderMessages();
						refreshControls();
					} else {
						if (updated != null) {
							state.select(updated);
						}
						loadPayments();
						messages.success(successMessage);
						renderMessages();
					}
				}));
	}

	// --- Editor helpers --------------------------------------------------------

	private void updateAmountPreview() {
		RentalResponseDto rental = rentalComboBox.getSelectionModel().getSelectedItem();
		if (rental == null || rental.totalPrice() == null) {
			amountPreviewLabel.setText("—");
			return;
		}
		amountPreviewLabel.setText(RentalFormatter.formatPrice(rental.totalPrice()) + " (from rental total)");
	}

	private void reselectRental(Long rentalId) {
		if (rentalId == null) {
			rentalComboBox.getSelectionModel().clearSelection();
			return;
		}
		rentalComboBox.getItems().stream()
				.filter(rental -> Objects.equals(rental.id(), rentalId))
				.findFirst()
				.ifPresentOrElse(
						rental -> rentalComboBox.getSelectionModel().select(rental),
						() -> rentalComboBox.getSelectionModel().clearSelection());
	}

	private Long selectedRentalId() {
		RentalResponseDto rental = rentalComboBox.getSelectionModel().getSelectedItem();
		return rental == null ? null : rental.id();
	}

	// --- Control state ---------------------------------------------------------

	private void refreshControls() {
		boolean readOnly = state.isReadOnly();
		setVisibleManaged(readOnlyNoticeLabel, readOnly);
		setVisibleManaged(addButton, !readOnly);
		setVisibleManaged(markPaidButton, !readOnly);
		setVisibleManaged(markFailedButton, !readOnly);
		setVisibleManaged(retryButton, !readOnly);
		setVisibleManaged(refundButton, state.canEverRefund());
		setVisibleManaged(deleteButton, state.canEverDelete());

		addButton.setDisable(!state.canCreate());
		markPaidButton.setDisable(!state.canMarkPaid());
		markFailedButton.setDisable(!state.canMarkFailed());
		retryButton.setDisable(!state.canRetry());
		refundButton.setDisable(!state.canRefund());
		deleteButton.setDisable(!state.canDelete());
		refreshButton.setDisable(!state.canRefresh());
		saveButton.setDisable(state.isBusy());
		cancelEditButton.setDisable(state.isBusy());

		loadingIndicator.setVisible(state.isLoading());
		setVisibleManaged(emptyStateLabel, state.isEmpty());
		setVisibleManaged(paymentEditor, state.isFormVisible());
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}

	// --- Display labels --------------------------------------------------------

	private String paymentRentalLabel(PaymentResponseDto payment) {
		RentalResponseDto rental = payment.rentalId() == null ? null : rentalsById.get(payment.rentalId());
		if (rental != null) {
			return rentalOptionLabel(rental);
		}
		String status = payment.rentalStatus() == null ? "" : " (" + payment.rentalStatus().displayLabel() + ")";
		return "Rental #" + payment.rentalId() + status;
	}

	private static String rentalOptionLabel(RentalResponseDto rental) {
		StringBuilder label = new StringBuilder("Rental #").append(rental.id());
		String vehicle = vehicleLabel(rental);
		if (!vehicle.isEmpty()) {
			label.append(" — ").append(vehicle);
		}
		String customer = customerLabel(rental);
		if (!customer.isEmpty()) {
			label.append(" — ").append(customer);
		}
		if (rental.status() != null) {
			label.append(" (").append(rental.status().displayLabel()).append(")");
		}
		return label.toString();
	}

	private static String vehicleLabel(RentalResponseDto rental) {
		String reg = rental.vehicleRegistrationNumber() == null ? "" : rental.vehicleRegistrationNumber();
		String make = ((rental.vehicleBrand() == null ? "" : rental.vehicleBrand()) + " "
				+ (rental.vehicleModel() == null ? "" : rental.vehicleModel())).trim();
		if (reg.isEmpty()) {
			return make;
		}
		return make.isEmpty() ? reg : reg + " " + make;
	}

	private static String customerLabel(RentalResponseDto rental) {
		String first = rental.customerFirstName() == null ? "" : rental.customerFirstName();
		String last = rental.customerLastName() == null ? "" : rental.customerLastName();
		return (first + " " + last).trim();
	}

	// --- Messages --------------------------------------------------------------

	/**
	 * Mirrors the {@link PaymentMessageState} onto the two message areas: one
	 * wrapping label per form error below the editor (never truncated) and the
	 * general status message above the table. Because the model keeps the two areas
	 * mutually exclusive, the same failure is never shown in both places.
	 */
	private void renderMessages() {
		formMessagesContainer.getChildren().clear();
		for (String line : messages.formMessages()) {
			Label label = new Label(line);
			label.setWrapText(true);
			label.setMaxWidth(Double.MAX_VALUE);
			label.getStyleClass().add("payment-validation-message");
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

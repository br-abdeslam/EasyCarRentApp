package be.condorcet.easycarrent.desktop.view;

import be.condorcet.easycarrent.desktop.dashboard.DashboardAggregator;
import be.condorcet.easycarrent.desktop.dashboard.DashboardSnapshot;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;
import be.condorcet.easycarrent.desktop.service.DashboardFormatter;
import be.condorcet.easycarrent.desktop.service.DashboardService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for the read-only Dashboard screen.
 *
 * <p>Instantiated by {@link javafx.fxml.FXMLLoader} via its public no-argument
 * constructor; its collaborator is supplied through {@link #init} after loading,
 * which also triggers the first asynchronous load. All work runs through
 * {@link DashboardService} (which concurrently loads the six domain lists) and the
 * pure {@link DashboardAggregator}; there is no direct HTTP, no blocking call, and
 * every UI update from an async completion is marshalled with
 * {@link Platform#runLater(Runnable)}. The screen has no write control other than
 * Refresh, shows every exact status (zero included), marks a failed source section as
 * "Unavailable" rather than zero, and displays no customer personal data. State is
 * conveyed through CSS classes.</p>
 */
public class DashboardController {

	private static final String METRIC_VALUE = "dashboard-metric-value";
	private static final String METRIC_UNAVAILABLE = "dashboard-metric-unavailable";
	private static final String STATUS_BASE = "dashboard-status-message";
	private static final String STATUS_WARNING = "dashboard-status-warning";

	@FXML
	private ProgressIndicator loadingIndicator;

	@FXML
	private Button refreshButton;

	@FXML
	private Label lastUpdatedLabel;

	@FXML
	private Label statusMessageLabel;

	@FXML
	private Label unavailableLabel;

	@FXML
	private VBox dashboardContent;

	@FXML
	private Label vehicleCategoryCountLabel;

	@FXML
	private Label vehicleCountLabel;

	@FXML
	private Label availableVehicleCountLabel;

	@FXML
	private Label customerCountLabel;

	@FXML
	private Label activeRentalCountLabel;

	@FXML
	private Label pendingPaymentCountLabel;

	@FXML
	private Label maintenanceInProgressCountLabel;

	@FXML
	private FlowPane vehicleStatusContainer;

	@FXML
	private FlowPane rentalStatusContainer;

	@FXML
	private FlowPane paymentStatusContainer;

	@FXML
	private FlowPane maintenanceStatusContainer;

	@FXML
	private Label paidPaymentAmountLabel;

	@FXML
	private Label pendingPaymentAmountLabel;

	@FXML
	private Label refundedPaymentAmountLabel;

	private DashboardService dashboardService;
	private final DashboardViewState state = new DashboardViewState();

	/** Supplies the collaborator after {@code FXMLLoader.load()} and starts loading. */
	public void init(DashboardService dashboardService) {
		this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
		render();
		triggerLoad();
	}

	@FXML
	private void handleRefresh() {
		triggerLoad();
	}

	private void triggerLoad() {
		if (dashboardService == null || !state.beginLoad()) {
			return;
		}
		render();
		dashboardService.load().whenComplete((result, throwable) -> Platform.runLater(() -> {
			if (throwable != null || result == null) {
				state.loadFailed();
			} else {
				DashboardSnapshot snapshot =
						DashboardAggregator.aggregate(result, LocalDateTime.now());
				state.loadSucceeded(snapshot, result.allAvailable(), result.anyAvailable());
			}
			render();
		}));
	}

	// --- Rendering -------------------------------------------------------------

	private void render() {
		loadingIndicator.setVisible(state.isLoading());
		refreshButton.setDisable(!state.canRefresh());

		boolean hasData = state.hasSnapshot();
		boolean fullyUnavailable =
				!hasData && state.statusKind() == DashboardViewState.StatusKind.UNAVAILABLE;
		setVisibleManaged(dashboardContent, hasData);
		setVisibleManaged(unavailableLabel, fullyUnavailable);

		if (hasData) {
			renderSnapshot(state.snapshot());
			lastUpdatedLabel.setText("Last updated: "
					+ DashboardFormatter.formatDateTime(state.snapshot().calculatedAt())
					+ " (local time)");
			setVisibleManaged(lastUpdatedLabel, true);
		} else {
			lastUpdatedLabel.setText("");
			setVisibleManaged(lastUpdatedLabel, false);
		}

		// The partial / incomplete-refresh banner is only meaningful when data is
		// shown; a fully-unavailable first load uses the prominent unavailable label.
		boolean showBanner = hasData
				&& (state.statusKind() == DashboardViewState.StatusKind.PARTIAL
						|| state.statusKind() == DashboardViewState.StatusKind.REFRESH_INCOMPLETE);
		statusMessageLabel.setText(showBanner ? state.statusMessage() : "");
		statusMessageLabel.getStyleClass().setAll(showBanner
				? new String[] {STATUS_BASE, STATUS_WARNING}
				: new String[] {STATUS_BASE});
		setVisibleManaged(statusMessageLabel, showBanner);
	}

	private void renderSnapshot(DashboardSnapshot snapshot) {
		setMetric(vehicleCategoryCountLabel, snapshot.categoriesAvailable(),
				snapshot.totalVehicleCategories());
		setMetric(vehicleCountLabel, snapshot.vehiclesAvailable(), snapshot.totalVehicles());
		setMetric(availableVehicleCountLabel, snapshot.vehiclesAvailable(),
				snapshot.availableVehicleCount());
		setMetric(customerCountLabel, snapshot.customersAvailable(), snapshot.totalCustomers());
		setMetric(activeRentalCountLabel, snapshot.rentalsAvailable(), snapshot.activeRentalCount());
		setMetric(pendingPaymentCountLabel, snapshot.paymentsAvailable(),
				snapshot.pendingPaymentCount());
		setMetric(maintenanceInProgressCountLabel, snapshot.maintenanceAvailable(),
				snapshot.maintenanceInProgressCount());

		renderStatusBreakdown(vehicleStatusContainer, snapshot.vehiclesAvailable(),
				VehicleStatus.values(), snapshot.vehicleCounts(), VehicleStatus::displayLabel);
		renderStatusBreakdown(rentalStatusContainer, snapshot.rentalsAvailable(),
				RentalStatus.values(), snapshot.rentalCounts(), RentalStatus::displayLabel);
		renderStatusBreakdown(paymentStatusContainer, snapshot.paymentsAvailable(),
				PaymentStatus.values(), snapshot.paymentCounts(), PaymentStatus::displayLabel);
		renderStatusBreakdown(maintenanceStatusContainer, snapshot.maintenanceAvailable(),
				MaintenanceStatus.values(), snapshot.maintenanceCounts(), MaintenanceStatus::displayLabel);

		renderAmount(paidPaymentAmountLabel, snapshot.paymentsAvailable(), snapshot.paidPaymentAmount());
		renderAmount(pendingPaymentAmountLabel, snapshot.paymentsAvailable(),
				snapshot.pendingPaymentAmount());
		renderAmount(refundedPaymentAmountLabel, snapshot.paymentsAvailable(),
				snapshot.refundedPaymentAmount());
	}

	private static void setMetric(Label label, boolean available, long count) {
		if (available) {
			label.setText(DashboardFormatter.formatCount(count));
			label.getStyleClass().setAll(METRIC_VALUE);
		} else {
			label.setText(DashboardFormatter.UNAVAILABLE);
			label.getStyleClass().setAll(METRIC_VALUE, METRIC_UNAVAILABLE);
		}
	}

	private static <E extends Enum<E>> void renderStatusBreakdown(FlowPane container, boolean available,
			E[] statuses, Map<E, Long> counts, Function<E, String> labelOf) {
		container.getChildren().clear();
		if (!available) {
			Label unavailable = new Label(DashboardFormatter.UNAVAILABLE);
			unavailable.getStyleClass().add(METRIC_UNAVAILABLE);
			container.getChildren().add(unavailable);
			return;
		}
		for (E status : statuses) {
			long count = counts.getOrDefault(status, 0L);
			container.getChildren().add(statusItem(labelOf.apply(status),
					DashboardFormatter.formatCount(count)));
		}
	}

	private static Node statusItem(String name, String value) {
		Label nameLabel = new Label(name);
		nameLabel.getStyleClass().add("dashboard-status-name");
		Label valueLabel = new Label(value);
		valueLabel.getStyleClass().add("dashboard-status-value");
		HBox item = new HBox(6.0, nameLabel, valueLabel);
		item.getStyleClass().add("dashboard-status-item");
		return item;
	}

	private static void renderAmount(Label label, boolean available, BigDecimal amount) {
		if (available) {
			label.setText(DashboardFormatter.formatAmount(amount));
			label.getStyleClass().setAll("dashboard-status-value");
		} else {
			label.setText(DashboardFormatter.UNAVAILABLE);
			label.getStyleClass().setAll("dashboard-status-value", METRIC_UNAVAILABLE);
		}
	}

	private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
		node.setVisible(visible);
		node.setManaged(visible);
	}
}

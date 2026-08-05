package be.condorcet.easycarrent.desktop.service;

import be.condorcet.easycarrent.desktop.dashboard.DashboardLoadResult;
import be.condorcet.easycarrent.desktop.dashboard.DashboardSectionResult;
import be.condorcet.easycarrent.desktop.http.ApiConnectionException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Read-only dashboard loading workflow.
 *
 * <p>Depends on the six existing domain services and loads their list operations
 * <em>concurrently</em>: each request is started before any result is awaited, and
 * results are combined with {@link CompletableFuture} composition without any
 * blocking call. Every section is wrapped so a technical failure (an
 * {@code ApiRequestException}, an {@code ApiConnectionException}, or any unexpected
 * completion failure) becomes a safe {@link DashboardSectionResult#unavailable}
 * rather than failing the whole load — one failing endpoint never hides the working
 * sections. It performs no writes, holds no JavaFX state, exposes no credentials,
 * and never puts an exception class name, {@code Authorization} value, or JSON into a
 * message. It does not retry, poll, or cache; each call performs a fresh load and the
 * controller/view-state prevents duplicate simultaneous refreshes.</p>
 */
public final class DashboardService {

	private static final String CONNECTION_UNAVAILABLE = "The backend is unavailable.";
	private static final String SECTION_UNAVAILABLE = "This section could not be loaded.";

	private final VehicleCategoryService vehicleCategoryService;
	private final VehicleService vehicleService;
	private final CustomerService customerService;
	private final RentalService rentalService;
	private final PaymentService paymentService;
	private final MaintenanceService maintenanceService;

	public DashboardService(VehicleCategoryService vehicleCategoryService,
			VehicleService vehicleService, CustomerService customerService,
			RentalService rentalService, PaymentService paymentService,
			MaintenanceService maintenanceService) {
		this.vehicleCategoryService =
				Objects.requireNonNull(vehicleCategoryService, "vehicleCategoryService");
		this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
		this.customerService = Objects.requireNonNull(customerService, "customerService");
		this.rentalService = Objects.requireNonNull(rentalService, "rentalService");
		this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
		this.maintenanceService = Objects.requireNonNull(maintenanceService, "maintenanceService");
	}

	/**
	 * Loads every section concurrently and combines them into one result. The
	 * returned future never completes exceptionally: a failed section is represented
	 * as unavailable inside the result.
	 */
	public CompletableFuture<DashboardLoadResult> load() {
		var categories = section(vehicleCategoryService::findAll);
		var vehicles = section(vehicleService::findAll);
		var customers = section(customerService::findAll);
		var rentals = section(rentalService::findAll);
		var payments = section(paymentService::findAll);
		var maintenance = section(maintenanceService::findAll);

		// All six requests are already in flight; combine once every section has
		// settled. Because each section future is non-exceptional, getNow returns the
		// settled value without blocking.
		return CompletableFuture.allOf(categories, vehicles, customers, rentals, payments, maintenance)
				.thenApply(ignored -> new DashboardLoadResult(
						categories.getNow(null), vehicles.getNow(null), customers.getNow(null),
						rentals.getNow(null), payments.getNow(null), maintenance.getNow(null)));
	}

	/**
	 * Starts one section request and converts any synchronous or asynchronous failure
	 * into a safe unavailable section, so the combined load never fails as a whole.
	 */
	private <T> CompletableFuture<DashboardSectionResult<T>> section(
			Supplier<CompletableFuture<List<T>>> call) {
		try {
			return call.get()
					.thenApply(DashboardSectionResult::available)
					.exceptionally(throwable ->
							DashboardSectionResult.unavailable(safeSectionMessage(throwable)));
		} catch (RuntimeException synchronousFailure) {
			return CompletableFuture.completedFuture(
					DashboardSectionResult.unavailable(safeSectionMessage(synchronousFailure)));
		}
	}

	private static String safeSectionMessage(Throwable throwable) {
		Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
				? throwable.getCause()
				: throwable;
		if (cause instanceof ApiConnectionException) {
			return CONNECTION_UNAVAILABLE;
		}
		return SECTION_UNAVAILABLE;
	}
}

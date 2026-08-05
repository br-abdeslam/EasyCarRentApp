package be.condorcet.easycarrent.desktop.dashboard;

import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, JavaFX-free computed dashboard data.
 *
 * <p>Holds only display-ready aggregates: per-domain totals, exact per-status
 * breakdowns (every backend enum constant is present, with zero when absent), and
 * the approved payment-amount summaries. Each section also records whether it was
 * available in the load that produced this snapshot, so a preserved snapshot can be
 * rendered correctly (a failed section is shown as "Unavailable", never as zero).
 * Counts are {@code long} and never negative; amounts are {@link BigDecimal}; no
 * DTO lists, customer personal data, credentials, or JavaFX types are retained. The
 * calculation time is supplied explicitly (client refresh time, not server time).</p>
 */
public final class DashboardSnapshot {

	private final LocalDateTime calculatedAt;

	private final boolean categoriesAvailable;
	private final boolean vehiclesAvailable;
	private final boolean customersAvailable;
	private final boolean rentalsAvailable;
	private final boolean paymentsAvailable;
	private final boolean maintenanceAvailable;

	private final long totalVehicleCategories;
	private final long totalVehicles;
	private final long totalCustomers;
	private final long totalRentals;
	private final long totalPayments;
	private final long totalMaintenanceRecords;

	private final Map<VehicleStatus, Long> vehicleCounts;
	private final Map<RentalStatus, Long> rentalCounts;
	private final Map<PaymentStatus, Long> paymentCounts;
	private final Map<MaintenanceStatus, Long> maintenanceCounts;

	private final BigDecimal paidPaymentAmount;
	private final BigDecimal pendingPaymentAmount;
	private final BigDecimal refundedPaymentAmount;

	private DashboardSnapshot(Builder builder) {
		this.calculatedAt = builder.calculatedAt;
		this.categoriesAvailable = builder.categoriesAvailable;
		this.vehiclesAvailable = builder.vehiclesAvailable;
		this.customersAvailable = builder.customersAvailable;
		this.rentalsAvailable = builder.rentalsAvailable;
		this.paymentsAvailable = builder.paymentsAvailable;
		this.maintenanceAvailable = builder.maintenanceAvailable;
		this.totalVehicleCategories = requireNonNegative(builder.totalVehicleCategories, "categories");
		this.totalVehicles = requireNonNegative(builder.totalVehicles, "vehicles");
		this.totalCustomers = requireNonNegative(builder.totalCustomers, "customers");
		this.totalRentals = requireNonNegative(builder.totalRentals, "rentals");
		this.totalPayments = requireNonNegative(builder.totalPayments, "payments");
		this.totalMaintenanceRecords = requireNonNegative(builder.totalMaintenanceRecords, "maintenance");
		this.vehicleCounts = completeCounts(builder.vehicleCounts, VehicleStatus.class);
		this.rentalCounts = completeCounts(builder.rentalCounts, RentalStatus.class);
		this.paymentCounts = completeCounts(builder.paymentCounts, PaymentStatus.class);
		this.maintenanceCounts = completeCounts(builder.maintenanceCounts, MaintenanceStatus.class);
		this.paidPaymentAmount = Objects.requireNonNull(builder.paidPaymentAmount, "paidPaymentAmount");
		this.pendingPaymentAmount =
				Objects.requireNonNull(builder.pendingPaymentAmount, "pendingPaymentAmount");
		this.refundedPaymentAmount =
				Objects.requireNonNull(builder.refundedPaymentAmount, "refundedPaymentAmount");
	}

	private static long requireNonNegative(long value, String name) {
		if (value < 0) {
			throw new IllegalArgumentException("total " + name + " must not be negative: " + value);
		}
		return value;
	}

	/** Copies the given counts and fills every enum constant that is absent with zero. */
	private static <E extends Enum<E>> Map<E, Long> completeCounts(Map<E, Long> source,
			Class<E> type) {
		Map<E, Long> complete = new EnumMap<>(type);
		for (E constant : type.getEnumConstants()) {
			complete.put(constant, 0L);
		}
		if (source != null) {
			source.forEach((status, count) -> {
				if (count != null) {
					complete.put(status, requireNonNegative(count, status.name()));
				}
			});
		}
		return complete;
	}

	public LocalDateTime calculatedAt() {
		return calculatedAt;
	}

	public boolean categoriesAvailable() {
		return categoriesAvailable;
	}

	public boolean vehiclesAvailable() {
		return vehiclesAvailable;
	}

	public boolean customersAvailable() {
		return customersAvailable;
	}

	public boolean rentalsAvailable() {
		return rentalsAvailable;
	}

	public boolean paymentsAvailable() {
		return paymentsAvailable;
	}

	public boolean maintenanceAvailable() {
		return maintenanceAvailable;
	}

	public long totalVehicleCategories() {
		return totalVehicleCategories;
	}

	public long totalVehicles() {
		return totalVehicles;
	}

	public long totalCustomers() {
		return totalCustomers;
	}

	public long totalRentals() {
		return totalRentals;
	}

	public long totalPayments() {
		return totalPayments;
	}

	public long totalMaintenanceRecords() {
		return totalMaintenanceRecords;
	}

	/** @return an unmodifiable map with a count for every {@link VehicleStatus}. */
	public Map<VehicleStatus, Long> vehicleCounts() {
		return Map.copyOf(vehicleCounts);
	}

	public Map<RentalStatus, Long> rentalCounts() {
		return Map.copyOf(rentalCounts);
	}

	public Map<PaymentStatus, Long> paymentCounts() {
		return Map.copyOf(paymentCounts);
	}

	public Map<MaintenanceStatus, Long> maintenanceCounts() {
		return Map.copyOf(maintenanceCounts);
	}

	public long availableVehicleCount() {
		return vehicleCounts.getOrDefault(VehicleStatus.AVAILABLE, 0L);
	}

	public long activeRentalCount() {
		return rentalCounts.getOrDefault(RentalStatus.ACTIVE, 0L);
	}

	public long pendingPaymentCount() {
		return paymentCounts.getOrDefault(PaymentStatus.PENDING, 0L);
	}

	public long maintenanceInProgressCount() {
		return maintenanceCounts.getOrDefault(MaintenanceStatus.IN_PROGRESS, 0L);
	}

	public BigDecimal paidPaymentAmount() {
		return paidPaymentAmount;
	}

	public BigDecimal pendingPaymentAmount() {
		return pendingPaymentAmount;
	}

	public BigDecimal refundedPaymentAmount() {
		return refundedPaymentAmount;
	}

	/** A fluent builder used by the {@link DashboardAggregator}. */
	public static final class Builder {

		private LocalDateTime calculatedAt;
		private boolean categoriesAvailable;
		private boolean vehiclesAvailable;
		private boolean customersAvailable;
		private boolean rentalsAvailable;
		private boolean paymentsAvailable;
		private boolean maintenanceAvailable;
		private long totalVehicleCategories;
		private long totalVehicles;
		private long totalCustomers;
		private long totalRentals;
		private long totalPayments;
		private long totalMaintenanceRecords;
		private Map<VehicleStatus, Long> vehicleCounts;
		private Map<RentalStatus, Long> rentalCounts;
		private Map<PaymentStatus, Long> paymentCounts;
		private Map<MaintenanceStatus, Long> maintenanceCounts;
		private BigDecimal paidPaymentAmount = BigDecimal.ZERO;
		private BigDecimal pendingPaymentAmount = BigDecimal.ZERO;
		private BigDecimal refundedPaymentAmount = BigDecimal.ZERO;

		public Builder calculatedAt(LocalDateTime calculatedAt) {
			this.calculatedAt = calculatedAt;
			return this;
		}

		public Builder categories(boolean available, long total) {
			this.categoriesAvailable = available;
			this.totalVehicleCategories = total;
			return this;
		}

		public Builder vehicles(boolean available, long total, Map<VehicleStatus, Long> counts) {
			this.vehiclesAvailable = available;
			this.totalVehicles = total;
			this.vehicleCounts = counts;
			return this;
		}

		public Builder customers(boolean available, long total) {
			this.customersAvailable = available;
			this.totalCustomers = total;
			return this;
		}

		public Builder rentals(boolean available, long total, Map<RentalStatus, Long> counts) {
			this.rentalsAvailable = available;
			this.totalRentals = total;
			this.rentalCounts = counts;
			return this;
		}

		public Builder payments(boolean available, long total, Map<PaymentStatus, Long> counts,
				BigDecimal paid, BigDecimal pending, BigDecimal refunded) {
			this.paymentsAvailable = available;
			this.totalPayments = total;
			this.paymentCounts = counts;
			this.paidPaymentAmount = paid;
			this.pendingPaymentAmount = pending;
			this.refundedPaymentAmount = refunded;
			return this;
		}

		public Builder maintenance(boolean available, long total, Map<MaintenanceStatus, Long> counts) {
			this.maintenanceAvailable = available;
			this.totalMaintenanceRecords = total;
			this.maintenanceCounts = counts;
			return this;
		}

		public DashboardSnapshot build() {
			return new DashboardSnapshot(this);
		}
	}
}

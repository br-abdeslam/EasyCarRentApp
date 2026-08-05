package be.condorcet.easycarrent.desktop.dashboard;

import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Pure, JavaFX-free aggregation of the six domain lists into a
 * {@link DashboardSnapshot}.
 *
 * <p>It makes no HTTP calls, keeps no state, and uses no floating-point arithmetic.
 * Each record is counted exactly once by the status the backend DTO reports (status
 * is never inferred from dates, and vehicle availability is never inferred from
 * rentals). Every backend enum constant appears in its breakdown, with zero when
 * absent, and an empty successful list yields zeros — distinct from an unavailable
 * section, which is marked unavailable rather than zero. Payment amounts are summed
 * with {@link BigDecimal} per status: PAID, PENDING, and REFUNDED each contribute
 * only to their own total, FAILED contributes to none, and a null amount (which the
 * contract does not produce, but is handled defensively) contributes nothing. Only a
 * customer count is taken; no customer personal data enters the snapshot. Input
 * lists are never mutated.</p>
 */
public final class DashboardAggregator {

	private DashboardAggregator() {
	}

	/**
	 * Aggregates the load result into a snapshot.
	 *
	 * @param result       the six section results (each available or unavailable)
	 * @param calculatedAt the client refresh time to stamp on the snapshot
	 * @return the computed snapshot
	 */
	public static DashboardSnapshot aggregate(DashboardLoadResult result, LocalDateTime calculatedAt) {
		Objects.requireNonNull(result, "result");
		Objects.requireNonNull(calculatedAt, "calculatedAt");

		DashboardSnapshot.Builder builder = new DashboardSnapshot.Builder().calculatedAt(calculatedAt);

		if (result.categories().isAvailable()) {
			builder.categories(true, result.categories().records().size());
		} else {
			builder.categories(false, 0L);
		}

		if (result.vehicles().isAvailable()) {
			List<VehicleResponseDto> vehicles = result.vehicles().records();
			builder.vehicles(true, vehicles.size(),
					countBy(vehicles, VehicleResponseDto::status, VehicleStatus.class));
		} else {
			builder.vehicles(false, 0L, Map.of());
		}

		if (result.customers().isAvailable()) {
			builder.customers(true, result.customers().records().size());
		} else {
			builder.customers(false, 0L);
		}

		if (result.rentals().isAvailable()) {
			List<RentalResponseDto> rentals = result.rentals().records();
			builder.rentals(true, rentals.size(),
					countBy(rentals, RentalResponseDto::status, RentalStatus.class));
		} else {
			builder.rentals(false, 0L, Map.of());
		}

		if (result.payments().isAvailable()) {
			List<PaymentResponseDto> payments = result.payments().records();
			builder.payments(true, payments.size(),
					countBy(payments, PaymentResponseDto::status, PaymentStatus.class),
					sumAmounts(payments, PaymentStatus.PAID),
					sumAmounts(payments, PaymentStatus.PENDING),
					sumAmounts(payments, PaymentStatus.REFUNDED));
		} else {
			builder.payments(false, 0L, Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		}

		if (result.maintenance().isAvailable()) {
			List<MaintenanceResponseDto> maintenance = result.maintenance().records();
			builder.maintenance(true, maintenance.size(),
					countBy(maintenance, MaintenanceResponseDto::status, MaintenanceStatus.class));
		} else {
			builder.maintenance(false, 0L, Map.of());
		}

		return builder.build();
	}

	private static <T, E extends Enum<E>> Map<E, Long> countBy(List<T> records,
			Function<T, E> statusOf, Class<E> type) {
		Map<E, Long> counts = new EnumMap<>(type);
		for (E constant : type.getEnumConstants()) {
			counts.put(constant, 0L);
		}
		for (T record : records) {
			E status = statusOf.apply(record);
			if (status != null) {
				counts.merge(status, 1L, Long::sum);
			}
		}
		return counts;
	}

	private static BigDecimal sumAmounts(List<PaymentResponseDto> payments, PaymentStatus status) {
		BigDecimal total = BigDecimal.ZERO;
		for (PaymentResponseDto payment : payments) {
			if (payment.status() == status && payment.amount() != null) {
				total = total.add(payment.amount());
			}
		}
		return total;
	}
}

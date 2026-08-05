package be.condorcet.easycarrent.desktop.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DashboardSnapshotTest {

	private static final LocalDateTime AT = LocalDateTime.of(2026, 8, 5, 9, 30);

	@Test
	void aMinimalSnapshotIsAllZeroAllUnavailableWithEveryStatusKey() {
		DashboardSnapshot snapshot = new DashboardSnapshot.Builder().calculatedAt(AT).build();

		assertEquals(AT, snapshot.calculatedAt());
		assertEquals(0, snapshot.totalVehicles());
		assertFalse(snapshot.vehiclesAvailable());
		assertEquals(4, snapshot.vehicleCounts().size());
		assertEquals(4, snapshot.paymentCounts().size());
		assertEquals(3, snapshot.maintenanceCounts().size());
		assertEquals(0L, snapshot.vehicleCounts().get(VehicleStatus.AVAILABLE));
		assertEquals(0, BigDecimal.ZERO.compareTo(snapshot.paidPaymentAmount()));
	}

	@Test
	void statusCountMapsAreUnmodifiable() {
		DashboardSnapshot snapshot = new DashboardSnapshot.Builder().calculatedAt(AT).build();
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.vehicleCounts().put(VehicleStatus.AVAILABLE, 5L));
	}

	@Test
	void negativeTotalIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new DashboardSnapshot.Builder().calculatedAt(AT).categories(true, -1L).build());
	}

	@Test
	void negativeStatusCountIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new DashboardSnapshot.Builder()
				.calculatedAt(AT)
				.vehicles(true, 1L, Map.of(VehicleStatus.AVAILABLE, -1L))
				.build());
	}

	@Test
	void monetaryAmountsAreStoredExactlyAsBigDecimal() {
		DashboardSnapshot snapshot = new DashboardSnapshot.Builder().calculatedAt(AT)
				.payments(true, 3L, Map.of(PaymentStatus.PAID, 2L),
						new BigDecimal("30.30"), new BigDecimal("5.05"), new BigDecimal("7.00"))
				.build();

		assertEquals(new BigDecimal("30.30"), snapshot.paidPaymentAmount());
		assertEquals(new BigDecimal("5.05"), snapshot.pendingPaymentAmount());
		assertEquals(new BigDecimal("7.00"), snapshot.refundedPaymentAmount());
	}

	@Test
	void suppliedCountsAreCompletedWithZeroForAbsentConstants() {
		DashboardSnapshot snapshot = new DashboardSnapshot.Builder().calculatedAt(AT)
				.vehicles(true, 2L, Map.of(VehicleStatus.AVAILABLE, 2L))
				.build();

		assertEquals(2L, snapshot.vehicleCounts().get(VehicleStatus.AVAILABLE));
		assertEquals(0L, snapshot.vehicleCounts().get(VehicleStatus.RENTED));
		assertTrue(snapshot.vehicleCounts().containsKey(VehicleStatus.INACTIVE));
	}
}

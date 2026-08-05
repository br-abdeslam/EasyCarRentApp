package be.condorcet.easycarrent.desktop.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceStatus;
import be.condorcet.easycarrent.desktop.dto.PaymentMethod;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentStatus;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalStatus;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardAggregatorTest {

	private static final LocalDateTime AT = LocalDateTime.of(2026, 8, 5, 9, 30);

	// --- fictional-data factories ---------------------------------------------

	private static VehicleCategoryResponseDto category(long id) {
		return new VehicleCategoryResponseDto(id, "Category " + id, "desc");
	}

	private static VehicleResponseDto vehicle(long id, VehicleStatus status) {
		return new VehicleResponseDto(id, "REG-" + id, "Brand", "Model", 2022, "Blue",
				new BigDecimal("50.00"), 1000L, status, 1L, "Category 1");
	}

	private static CustomerResponseDto customer(long id) {
		return new CustomerResponseDto(id, "First", "Last", "test@example.invalid", "+0000000000",
				"1 Example Street", "LIC-" + id, LocalDate.of(2030, 1, 1));
	}

	private static RentalResponseDto rental(long id, RentalStatus status) {
		return new RentalResponseDto(id, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3), status,
				new BigDecimal("100.00"), LocalDateTime.of(2026, 8, 1, 10, 0), 1L, "REG-1", "Brand",
				"Model", 1L, "First", "Last");
	}

	private static PaymentResponseDto payment(long id, PaymentStatus status, String amount) {
		return new PaymentResponseDto(id, id, RentalStatus.ACTIVE, new BigDecimal(amount),
				PaymentMethod.CARD, status, LocalDateTime.of(2026, 8, 1, 10, 0), null);
	}

	private static MaintenanceResponseDto maintenance(long id, MaintenanceStatus status) {
		return new MaintenanceResponseDto(id, 1L, "Inspection", LocalDate.of(2027, 1, 1),
				LocalDate.of(2027, 1, 2), new BigDecimal("30.00"), status);
	}

	private static DashboardLoadResult available(List<VehicleCategoryResponseDto> categories,
			List<VehicleResponseDto> vehicles, List<CustomerResponseDto> customers,
			List<RentalResponseDto> rentals, List<PaymentResponseDto> payments,
			List<MaintenanceResponseDto> maintenance) {
		return new DashboardLoadResult(
				DashboardSectionResult.available(categories),
				DashboardSectionResult.available(vehicles),
				DashboardSectionResult.available(customers),
				DashboardSectionResult.available(rentals),
				DashboardSectionResult.available(payments),
				DashboardSectionResult.available(maintenance));
	}

	// --- tests -----------------------------------------------------------------

	@Test
	void emptyDataYieldsZerosEverywhereWithEveryStatusKey() {
		DashboardSnapshot snapshot = DashboardAggregator.aggregate(
				available(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()), AT);

		assertEquals(0, snapshot.totalVehicleCategories());
		assertEquals(0, snapshot.totalVehicles());
		assertEquals(0, snapshot.totalCustomers());
		assertEquals(0, snapshot.totalRentals());
		assertEquals(0, snapshot.totalPayments());
		assertEquals(0, snapshot.totalMaintenanceRecords());
		assertEquals(4, snapshot.vehicleCounts().size());
		assertEquals(4, snapshot.rentalCounts().size());
		assertEquals(4, snapshot.paymentCounts().size());
		assertEquals(3, snapshot.maintenanceCounts().size());
		assertTrue(snapshot.vehicleCounts().values().stream().allMatch(count -> count == 0L));
		assertEquals(0, BigDecimal.ZERO.compareTo(snapshot.paidPaymentAmount()));
		assertEquals(AT, snapshot.calculatedAt());
		assertTrue(snapshot.vehiclesAvailable());
	}

	@Test
	void everyEnumConstantIsPresentEvenWhenAbsentFromData() {
		DashboardSnapshot snapshot = DashboardAggregator.aggregate(
				available(List.of(), List.of(vehicle(1, VehicleStatus.AVAILABLE)), List.of(),
						List.of(rental(1, RentalStatus.ACTIVE)),
						List.of(payment(1, PaymentStatus.PAID, "10.00")),
						List.of(maintenance(1, MaintenanceStatus.PLANNED))),
				AT);

		for (VehicleStatus status : VehicleStatus.values()) {
			assertTrue(snapshot.vehicleCounts().containsKey(status), "missing " + status);
		}
		assertEquals(0L, snapshot.vehicleCounts().get(VehicleStatus.INACTIVE));
		assertEquals(0L, snapshot.rentalCounts().get(RentalStatus.CANCELLED));
		assertEquals(0L, snapshot.paymentCounts().get(PaymentStatus.FAILED));
		assertEquals(0L, snapshot.maintenanceCounts().get(MaintenanceStatus.COMPLETED));
	}

	@Test
	void countsEachRecordExactlyOnceAndHeadlineMetricsUseTheExactStatus() {
		List<VehicleResponseDto> vehicles = List.of(
				vehicle(1, VehicleStatus.AVAILABLE), vehicle(2, VehicleStatus.AVAILABLE),
				vehicle(3, VehicleStatus.RENTED), vehicle(4, VehicleStatus.MAINTENANCE));
		List<RentalResponseDto> rentals = List.of(
				rental(1, RentalStatus.ACTIVE), rental(2, RentalStatus.PLANNED),
				rental(3, RentalStatus.COMPLETED));
		List<PaymentResponseDto> payments = List.of(
				payment(1, PaymentStatus.PENDING, "10.00"), payment(2, PaymentStatus.PAID, "20.00"));
		List<MaintenanceResponseDto> maintenance = List.of(
				maintenance(1, MaintenanceStatus.IN_PROGRESS), maintenance(2, MaintenanceStatus.PLANNED));

		DashboardSnapshot snapshot = DashboardAggregator.aggregate(
				available(List.of(category(1)), vehicles, List.of(customer(1), customer(2)),
						rentals, payments, maintenance),
				AT);

		assertEquals(4, snapshot.totalVehicles());
		assertEquals(2, snapshot.availableVehicleCount());
		assertEquals(1, snapshot.vehicleCounts().get(VehicleStatus.RENTED));
		assertEquals(3, snapshot.totalRentals());
		assertEquals(1, snapshot.activeRentalCount());
		assertEquals(1, snapshot.pendingPaymentCount());
		assertEquals(1, snapshot.maintenanceInProgressCount());
		assertEquals(1, snapshot.totalVehicleCategories());
		assertEquals(2, snapshot.totalCustomers());
	}

	@Test
	void paymentAmountsAreSummedPerStatusWithFailedContributingNothing() {
		List<PaymentResponseDto> payments = List.of(
				payment(1, PaymentStatus.PAID, "10.10"),
				payment(2, PaymentStatus.PAID, "20.20"),
				payment(3, PaymentStatus.PENDING, "5.05"),
				payment(4, PaymentStatus.REFUNDED, "7.00"),
				payment(5, PaymentStatus.FAILED, "999.99"));

		DashboardSnapshot snapshot = DashboardAggregator.aggregate(
				available(List.of(), List.of(), List.of(), List.of(), payments, List.of()), AT);

		assertEquals(0, new BigDecimal("30.30").compareTo(snapshot.paidPaymentAmount()));
		assertEquals(0, new BigDecimal("5.05").compareTo(snapshot.pendingPaymentAmount()));
		assertEquals(0, new BigDecimal("7.00").compareTo(snapshot.refundedPaymentAmount()));
	}

	@Test
	void unavailableSectionIsMarkedUnavailableNotZero() {
		DashboardLoadResult result = new DashboardLoadResult(
				DashboardSectionResult.available(List.of(category(1))),
				DashboardSectionResult.unavailable("Vehicles unavailable"),
				DashboardSectionResult.available(List.of(customer(1))),
				DashboardSectionResult.available(List.of()),
				DashboardSectionResult.available(List.of()),
				DashboardSectionResult.available(List.of()));

		DashboardSnapshot snapshot = DashboardAggregator.aggregate(result, AT);

		assertFalse(snapshot.vehiclesAvailable(), "a failed section is unavailable, not zero");
		assertTrue(snapshot.categoriesAvailable());
		assertTrue(snapshot.customersAvailable());
		assertEquals(1, snapshot.totalVehicleCategories());
	}

	@Test
	void inputListsAreNotMutated() {
		List<VehicleResponseDto> vehicles = new ArrayList<>();
		vehicles.add(vehicle(1, VehicleStatus.AVAILABLE));
		DashboardAggregator.aggregate(
				available(List.of(), vehicles, List.of(), List.of(), List.of(), List.of()), AT);
		assertEquals(1, vehicles.size());
	}
}

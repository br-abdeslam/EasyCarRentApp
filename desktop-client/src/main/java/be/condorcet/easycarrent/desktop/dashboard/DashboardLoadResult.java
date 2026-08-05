package be.condorcet.easycarrent.desktop.dashboard;

import be.condorcet.easycarrent.desktop.dto.CustomerResponseDto;
import be.condorcet.easycarrent.desktop.dto.MaintenanceResponseDto;
import be.condorcet.easycarrent.desktop.dto.PaymentResponseDto;
import be.condorcet.easycarrent.desktop.dto.RentalResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleCategoryResponseDto;
import be.condorcet.easycarrent.desktop.dto.VehicleResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The combined outcome of one dashboard load: an individual
 * {@link DashboardSectionResult} for each of the six domain sections.
 *
 * <p>Immutable and JavaFX-free. Each section is preserved independently so that one
 * failing endpoint never hides the working sections. It exposes only booleans and
 * safe section names for the view; it never carries raw exceptions or credentials.</p>
 */
public final class DashboardLoadResult {

	private final DashboardSectionResult<VehicleCategoryResponseDto> categories;
	private final DashboardSectionResult<VehicleResponseDto> vehicles;
	private final DashboardSectionResult<CustomerResponseDto> customers;
	private final DashboardSectionResult<RentalResponseDto> rentals;
	private final DashboardSectionResult<PaymentResponseDto> payments;
	private final DashboardSectionResult<MaintenanceResponseDto> maintenance;

	public DashboardLoadResult(DashboardSectionResult<VehicleCategoryResponseDto> categories,
			DashboardSectionResult<VehicleResponseDto> vehicles,
			DashboardSectionResult<CustomerResponseDto> customers,
			DashboardSectionResult<RentalResponseDto> rentals,
			DashboardSectionResult<PaymentResponseDto> payments,
			DashboardSectionResult<MaintenanceResponseDto> maintenance) {
		this.categories = Objects.requireNonNull(categories, "categories");
		this.vehicles = Objects.requireNonNull(vehicles, "vehicles");
		this.customers = Objects.requireNonNull(customers, "customers");
		this.rentals = Objects.requireNonNull(rentals, "rentals");
		this.payments = Objects.requireNonNull(payments, "payments");
		this.maintenance = Objects.requireNonNull(maintenance, "maintenance");
	}

	public DashboardSectionResult<VehicleCategoryResponseDto> categories() {
		return categories;
	}

	public DashboardSectionResult<VehicleResponseDto> vehicles() {
		return vehicles;
	}

	public DashboardSectionResult<CustomerResponseDto> customers() {
		return customers;
	}

	public DashboardSectionResult<RentalResponseDto> rentals() {
		return rentals;
	}

	public DashboardSectionResult<PaymentResponseDto> payments() {
		return payments;
	}

	public DashboardSectionResult<MaintenanceResponseDto> maintenance() {
		return maintenance;
	}

	/** @return true if every section loaded successfully. */
	public boolean allAvailable() {
		return categories.isAvailable() && vehicles.isAvailable() && customers.isAvailable()
				&& rentals.isAvailable() && payments.isAvailable() && maintenance.isAvailable();
	}

	/** @return true if at least one section loaded successfully. */
	public boolean anyAvailable() {
		return categories.isAvailable() || vehicles.isAvailable() || customers.isAvailable()
				|| rentals.isAvailable() || payments.isAvailable() || maintenance.isAvailable();
	}

	/** @return true if no section loaded successfully. */
	public boolean allUnavailable() {
		return !anyAvailable();
	}

	/** @return the display names of the sections that failed to load, in a fixed order. */
	public List<String> unavailableSectionNames() {
		List<String> names = new ArrayList<>();
		if (!categories.isAvailable()) {
			names.add("Vehicle categories");
		}
		if (!vehicles.isAvailable()) {
			names.add("Vehicles");
		}
		if (!customers.isAvailable()) {
			names.add("Customers");
		}
		if (!rentals.isAvailable()) {
			names.add("Rentals");
		}
		if (!payments.isAvailable()) {
			names.add("Payments");
		}
		if (!maintenance.isAvailable()) {
			names.add("Maintenance");
		}
		return List.copyOf(names);
	}
}

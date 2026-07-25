package be.condorcet.easycarrent.desktop.navigation;

/**
 * The main navigable sections of the desktop application shell.
 *
 * <p>Each value carries only display metadata (a title and a short description)
 * used by the placeholder view. The descriptions state that each section is
 * prepared for future functionality; no section performs API calls or holds
 * domain data yet. This enum depends on no JavaFX controls.</p>
 */
public enum MainSection {

	DASHBOARD("Dashboard", "Application overview will be available here."),
	VEHICLE_CATEGORIES("Vehicle Categories", "Vehicle category management will be available here."),
	VEHICLES("Vehicles", "Vehicle management will be available here."),
	CUSTOMERS("Customers", "Customer management will be available here."),
	RENTALS("Rentals", "Rental management will be available here."),
	PAYMENTS("Payments", "Payment management will be available here."),
	MAINTENANCE("Maintenance", "Maintenance management will be available here.");

	private final String title;
	private final String description;

	MainSection(String title, String description) {
		this.title = title;
		this.description = description;
	}

	/** @return the human-readable section title */
	public String title() {
		return title;
	}

	/** @return the short, forward-looking section description */
	public String description() {
		return description;
	}
}

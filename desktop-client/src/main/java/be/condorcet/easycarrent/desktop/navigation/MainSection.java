package be.condorcet.easycarrent.desktop.navigation;

/**
 * The main navigable sections of the desktop application shell.
 *
 * <p>Each value carries only display metadata: a title used by the sidebar and a
 * short description of what the section does. Every section now routes to its own
 * backend-connected view; the description is used by the reusable placeholder view,
 * which remains only as a defensive fallback. This enum depends on no JavaFX
 * controls.</p>
 */
public enum MainSection {

	DASHBOARD("Dashboard", "A read-only overview of the current operational data."),
	VEHICLE_CATEGORIES("Vehicle Categories", "Manage the vehicle categories."),
	VEHICLES("Vehicles", "Manage the vehicle fleet."),
	CUSTOMERS("Customers", "Manage the customers who can rent vehicles."),
	RENTALS("Rentals", "Book vehicles and manage each rental's lifecycle."),
	PAYMENTS("Payments", "Record and manage the payment for each rental."),
	MAINTENANCE("Maintenance", "Schedule and track maintenance for each vehicle.");

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

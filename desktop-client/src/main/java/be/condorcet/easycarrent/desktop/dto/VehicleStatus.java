package be.condorcet.easycarrent.desktop.dto;

/**
 * Lifecycle states a vehicle can be in, matching the backend {@code VehicleStatus}
 * enum exactly.
 *
 * <p>The constant names are the values serialized by the backend, so Jackson maps
 * them by name. A separate, human-readable {@link #displayLabel()} is provided for
 * the UI without affecting the JSON contract. The status is managed by the
 * backend (it is not part of the vehicle create/update request), so the client
 * only displays it.</p>
 */
public enum VehicleStatus {

	AVAILABLE("Available"),
	RENTED("Rented"),
	MAINTENANCE("Maintenance"),
	INACTIVE("Inactive");

	private final String displayLabel;

	VehicleStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/** @return a human-readable label for display */
	public String displayLabel() {
		return displayLabel;
	}
}

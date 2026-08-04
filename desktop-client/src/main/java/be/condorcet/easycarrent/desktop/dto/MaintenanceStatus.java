package be.condorcet.easycarrent.desktop.dto;

/**
 * Lifecycle states a maintenance record can be in, matching the backend
 * {@code MaintenanceStatus} enum exactly.
 *
 * <p>The constant names are the values serialized by the backend, so Jackson maps
 * them by name. A separate, human-readable {@link #displayLabel()} is provided for
 * the UI without affecting the JSON contract. The status is managed by the backend
 * (it is not part of the create request and changes only through the dedicated
 * start/complete endpoints), so the client only displays it.</p>
 */
public enum MaintenanceStatus {

	PLANNED("Planned"),
	IN_PROGRESS("In progress"),
	COMPLETED("Completed");

	private final String displayLabel;

	MaintenanceStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/** @return a human-readable label for display */
	public String displayLabel() {
		return displayLabel;
	}
}

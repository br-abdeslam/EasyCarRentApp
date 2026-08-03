package be.condorcet.easycarrent.desktop.dto;

/**
 * Lifecycle states a rental can be in, matching the backend {@code RentalStatus}
 * enum exactly.
 *
 * <p>The constant names are the values serialized by the backend, so Jackson maps
 * them by name. A separate, human-readable {@link #displayLabel()} is provided for
 * the UI without affecting the JSON contract. The status is managed by the backend
 * (it is not part of the rental create/update request and changes only through the
 * dedicated lifecycle endpoints), so the client only displays it.</p>
 */
public enum RentalStatus {

	PLANNED("Planned"),
	ACTIVE("Active"),
	COMPLETED("Completed"),
	CANCELLED("Cancelled");

	private final String displayLabel;

	RentalStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/** @return a human-readable label for display */
	public String displayLabel() {
		return displayLabel;
	}
}

package be.condorcet.easycarrent.desktop.dto;

/**
 * Lifecycle states a payment can be in, matching the backend {@code PaymentStatus}
 * enum exactly.
 *
 * <p>The constant names are the values serialized by the backend, so Jackson maps
 * them by name. A separate, human-readable {@link #displayLabel()} is provided for
 * the UI without affecting the JSON contract. The status is managed by the backend
 * (it is not part of the payment create request and changes only through the
 * dedicated lifecycle endpoints), so the client only displays it.</p>
 */
public enum PaymentStatus {

	PENDING("Pending"),
	PAID("Paid"),
	FAILED("Failed"),
	REFUNDED("Refunded");

	private final String displayLabel;

	PaymentStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/** @return a human-readable label for display */
	public String displayLabel() {
		return displayLabel;
	}
}

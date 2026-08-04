package be.condorcet.easycarrent.desktop.dto;

/**
 * Accepted payment methods, matching the backend {@code PaymentMethod} enum
 * exactly.
 *
 * <p>The constant names are the values serialized by the backend, so Jackson maps
 * them by name. A separate, human-readable {@link #displayLabel()} is provided for
 * the UI without affecting the JSON contract. Only the method category is used; no
 * card, bank, token, or external-provider data exists in this application.</p>
 */
public enum PaymentMethod {

	CASH("Cash"),
	CARD("Card"),
	BANK_TRANSFER("Bank transfer");

	private final String displayLabel;

	PaymentMethod(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/** @return a human-readable label for display */
	public String displayLabel() {
		return displayLabel;
	}
}

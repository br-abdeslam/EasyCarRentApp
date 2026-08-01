package be.condorcet.easycarrent.desktop.view;

import java.util.List;

/**
 * JavaFX-free placement model for the two Customer message areas.
 *
 * <p>Keeps the form (Add/Edit) errors and the general status/operation message in
 * one place and enforces that the same failure is never shown in both areas: form
 * errors go below the form and clear the status area, while a general status
 * message goes above the table and clears the form errors. The controller mirrors
 * this state onto the JavaFX controls, so the placement rules stay unit-testable
 * without the graphical toolkit.</p>
 */
public final class CustomerMessageState {

	/** The kind of general status message, if any. */
	public enum StatusKind {
		NONE,
		SUCCESS,
		ERROR
	}

	private List<String> formMessages = List.of();
	private String statusMessage = "";
	private StatusKind statusKind = StatusKind.NONE;

	/** Clears both areas (used on Add/Edit open, Cancel, and before each Save pass). */
	public void clearAll() {
		formMessages = List.of();
		statusMessage = "";
		statusKind = StatusKind.NONE;
	}

	/**
	 * Shows the given form errors below the form and clears the general status area,
	 * so a form failure is never duplicated above the table.
	 */
	public void formErrors(List<String> lines) {
		formMessages = List.copyOf(lines);
		statusMessage = "";
		statusKind = StatusKind.NONE;
	}

	/** Shows a general success message above the table and clears any form errors. */
	public void success(String message) {
		formMessages = List.of();
		statusMessage = message;
		statusKind = StatusKind.SUCCESS;
	}

	/**
	 * Shows a general error/operation message above the table (for list, refresh, and
	 * delete-conflict feedback) and clears any form errors.
	 */
	public void statusError(String message) {
		formMessages = List.of();
		statusMessage = message;
		statusKind = StatusKind.ERROR;
	}

	public List<String> formMessages() {
		return formMessages;
	}

	public String statusMessage() {
		return statusMessage;
	}

	public StatusKind statusKind() {
		return statusKind;
	}

	public boolean hasFormMessages() {
		return !formMessages.isEmpty();
	}

	public boolean hasStatusMessage() {
		return statusKind != StatusKind.NONE;
	}
}

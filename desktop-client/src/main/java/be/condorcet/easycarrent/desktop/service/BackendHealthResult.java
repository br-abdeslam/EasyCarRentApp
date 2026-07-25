package be.condorcet.easycarrent.desktop.service;

/**
 * Immutable, display-safe outcome of a backend connectivity check.
 *
 * @param status  the connectivity status
 * @param message a short message safe to show in the UI
 */
public record BackendHealthResult(Status status, String message) {

	/** Connectivity outcomes distinguished by the health check. */
	public enum Status {
		/** The backend responded successfully. */
		CONNECTED,
		/** No response could be obtained (offline, refused, timed out). */
		UNAVAILABLE,
		/** A response was received but was not the expected success. */
		UNEXPECTED
	}

	public static BackendHealthResult connected(String message) {
		return new BackendHealthResult(Status.CONNECTED, message);
	}

	public static BackendHealthResult unavailable(String message) {
		return new BackendHealthResult(Status.UNAVAILABLE, message);
	}

	public static BackendHealthResult unexpected(String message) {
		return new BackendHealthResult(Status.UNEXPECTED, message);
	}

	/** @return true only when the backend was reachable and healthy */
	public boolean isConnected() {
		return status == Status.CONNECTED;
	}
}

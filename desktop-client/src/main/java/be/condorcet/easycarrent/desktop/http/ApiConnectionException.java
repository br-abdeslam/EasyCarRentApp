package be.condorcet.easycarrent.desktop.http;

/**
 * Thrown when no HTTP response could be obtained from the backend.
 *
 * <p>Covers connection refused, timeouts, DNS/network failures, request
 * interruption, and any other transport-level inability to complete the
 * exchange. Kept distinct from {@link ApiRequestException}, which represents a
 * response that <em>was</em> received but carried a non-success status.</p>
 */
public class ApiConnectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ApiConnectionException(String message) {
		super(message);
	}

	public ApiConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}

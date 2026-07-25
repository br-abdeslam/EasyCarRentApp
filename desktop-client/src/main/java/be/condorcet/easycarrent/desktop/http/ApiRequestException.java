package be.condorcet.easycarrent.desktop.http;

import be.condorcet.easycarrent.desktop.dto.ApiErrorDto;

import java.util.Optional;

/**
 * Thrown when the backend returns a completed HTTP response with a non-success
 * (non-2xx) status.
 *
 * <p>Carries the HTTP status code, the parsed {@link ApiErrorDto} when the error
 * body could be read, and the request path. The message is always safe to show
 * to a user; no credentials, headers, stack traces, or internal server details
 * are retained.</p>
 */
public class ApiRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final transient ApiErrorDto apiError;
	private final String path;

	public ApiRequestException(int status, String message, ApiErrorDto apiError, String path) {
		super(message);
		this.status = status;
		this.apiError = apiError;
		this.path = path;
	}

	/** @return the HTTP status code of the failed response */
	public int status() {
		return status;
	}

	/** @return the parsed backend error payload, if one was available */
	public Optional<ApiErrorDto> apiError() {
		return Optional.ofNullable(apiError);
	}

	/** @return the request path that produced the error */
	public String path() {
		return path;
	}
}

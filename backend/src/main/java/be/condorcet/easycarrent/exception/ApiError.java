package be.condorcet.easycarrent.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Consistent error payload returned by the API for every handled failure.
 *
 * <p>{@code validationErrors} is only present for field-validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path,
                              Map<String, String> validationErrors) {
        return new ApiError(Instant.now().toString(), status, error, message, path, validationErrors);
    }
}

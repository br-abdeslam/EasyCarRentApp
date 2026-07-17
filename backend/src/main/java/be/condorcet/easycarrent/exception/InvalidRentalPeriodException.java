package be.condorcet.easycarrent.exception;

/**
 * Thrown when a rental period is invalid, for example when the end date is not
 * strictly after the start date. Translated to 400.
 */
public class InvalidRentalPeriodException extends RuntimeException {

    public InvalidRentalPeriodException(String message) {
        super(message);
    }
}

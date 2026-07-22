package be.condorcet.easycarrent.exception;

/**
 * Thrown when a client request is semantically invalid in a way that plain
 * field validation cannot express, for example a cross-field rule such as a
 * maintenance end date preceding its start date. Translated to 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

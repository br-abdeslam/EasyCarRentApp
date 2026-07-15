package be.condorcet.easycarrent.exception;

/**
 * Thrown when a requested resource does not exist. Translated to 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

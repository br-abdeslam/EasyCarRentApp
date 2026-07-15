package be.condorcet.easycarrent.exception;

/**
 * Thrown when an operation conflicts with the current state of related data
 * (for example deleting a category still referenced by vehicles). Translated
 * to 409.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}

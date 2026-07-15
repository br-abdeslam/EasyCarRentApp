package be.condorcet.easycarrent.exception;

/**
 * Thrown when creating or updating a resource would violate a uniqueness rule
 * (for example a duplicate category name or registration number). Translated
 * to 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

package be.condorcet.easycarrent.entity;

/**
 * Lifecycle states a payment can be in.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}

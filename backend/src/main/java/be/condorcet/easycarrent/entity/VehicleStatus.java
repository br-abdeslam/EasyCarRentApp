package be.condorcet.easycarrent.entity;

/**
 * Lifecycle states a vehicle can be in within the rental fleet.
 */
public enum VehicleStatus {
    AVAILABLE,
    RENTED,
    MAINTENANCE,
    INACTIVE
}

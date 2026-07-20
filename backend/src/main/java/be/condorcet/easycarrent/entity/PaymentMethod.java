package be.condorcet.easycarrent.entity;

/**
 * Accepted payment methods. Only the method category is stored; no card
 * numbers, bank details, tokens or external-provider data are kept.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    BANK_TRANSFER
}

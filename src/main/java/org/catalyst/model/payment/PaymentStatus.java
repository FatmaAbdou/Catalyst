package org.catalyst.model.payment;

/** Matches the values allowed by {@code payment_transactions.status}. */
public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED
}


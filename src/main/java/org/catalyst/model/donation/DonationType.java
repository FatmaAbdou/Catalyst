package org.catalyst.model.donation;

/** Matches the values allowed by {@code donations.donation_type}. */
public enum DonationType {
    MONETARY("Monetary"),
    IN_KIND("In-kind");

    private final String displayName;

    DonationType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

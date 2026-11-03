package org.catalyst.model.payment;

/**
 * Ways a donation can be paid. {@link #getDonationCategory()} is the value stored in
 * {@code monetary_donations.payment_method}, which only allows CASH, CHECK and ONLINE;
 * the exact method is kept in {@code payment_transactions.payment_method}.
 */
public enum PaymentMethod {
    CASH("Cash", "CASH", true),
    CHECK("Check", "CHECK", true),
    CARD("Card", "ONLINE", false),
    INSTAPAY("InstaPay", "ONLINE", false);

    private final String displayName;
    private final String donationCategory;
    private final boolean staffOnly;

    PaymentMethod(String displayName, String donationCategory, boolean staffOnly) {
        this.displayName = displayName;
        this.donationCategory = donationCategory;
        this.staffOnly = staffOnly;
    }

    public String getDonationCategory() {
        return donationCategory;
    }

    /** Cash and checks are handed over in person, so only a donation officer can record them. */
    public boolean isStaffOnly() {
        return staffOnly;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

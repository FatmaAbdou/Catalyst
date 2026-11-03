package org.catalyst.model.receipt;

import org.catalyst.model.donation.Donation;
import org.catalyst.model.user.Donor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The plain receipt with the donor, date and donation details. Decorators wrap this one. */
public final class BasicReceipt implements Receipt {
    private final Donation donation;

    public BasicReceipt(Donation donation) {
        this.donation = Objects.requireNonNull(donation, "A receipt needs a donation.");
    }

    @Override
    public String getReference() {
        return donation.getReference();
    }

    @Override
    public String PrintReceipt() {
        Donor donor = donation.getDonor();
        String organization = donor.getOrganization();
        String donorLine = organization == null || organization.isBlank()
                ? donor.getName()
                : donor.getName() + " (" + organization + ")";

        List<String> lines = new ArrayList<>();
        lines.add("Receipt no.: " + donation.getReference());
        lines.add("Date: " + donation.getDate());
        lines.add("Donor: " + donorLine);
        lines.add("Donation type: " + donation.getType());
        lines.addAll(donation.describe());
        return String.join("\n", lines);
    }
}

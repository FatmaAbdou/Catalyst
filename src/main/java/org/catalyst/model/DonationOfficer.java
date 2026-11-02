package org.catalyst.model;

/** Staff account responsible for donation work. */
public final class DonationOfficer extends User {
    public DonationOfficer(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.DONATION_OFFICER;
    }
}

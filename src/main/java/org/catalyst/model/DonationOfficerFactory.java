package org.catalyst.model;

/** Creates donation officer accounts. */
public final class DonationOfficerFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.DONATION_OFFICER;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new DonationOfficer(name, email, contactInfo);
    }
}

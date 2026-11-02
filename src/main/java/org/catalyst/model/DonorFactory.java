package org.catalyst.model;

/** Creates donor accounts. */
public final class DonorFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.DONOR;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new Donor(name, email, contactInfo, organization);
    }
}

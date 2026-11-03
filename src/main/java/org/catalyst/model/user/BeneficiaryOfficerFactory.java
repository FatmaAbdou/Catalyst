package org.catalyst.model.user;

/** Creates beneficiary officer accounts. */
public final class BeneficiaryOfficerFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.BENEFICIARY_OFFICER;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new BeneficiaryOfficer(name, email, contactInfo);
    }
}

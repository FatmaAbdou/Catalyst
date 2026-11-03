package org.catalyst.model;

/** Staff account responsible for beneficiary assistance records. */
public final class BeneficiaryOfficer extends User {
    public BeneficiaryOfficer(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.BENEFICIARY_OFFICER;
    }
}

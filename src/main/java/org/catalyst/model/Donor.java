package org.catalyst.model;

/** A donor account may represent an individual or an organization. */
public final class Donor extends User {
    private final String organization;

    public Donor(String name, String email, String contactInfo, String organization) {
        super(name, email, contactInfo);
        this.organization = organization;
    }

    public String getOrganization() {
        return organization;
    }

    @Override
    public UserType getUserType() {
        return UserType.DONOR;
    }
}

package org.catalyst.model;

/** Creates administrator accounts. */
public final class AdministratorFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.ADMINISTRATOR;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new Administrator(name, email, contactInfo);
    }
}

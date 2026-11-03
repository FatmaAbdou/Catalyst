package org.catalyst.model.user;

/** Base creator in the Factory Method pattern. */
public abstract class UserFactory {
    public abstract User createUser(
            String name, String email, String contactInfo, String organization);

    public abstract UserType getUserType();
}

package org.catalyst.model;

/** Base creator in the Factory Method pattern. */
public abstract class UserFactory {
    public abstract User createUser(
            String name, String email, String contactInfo, String organization);

    public abstract UserType getUserType();
}

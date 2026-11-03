package org.catalyst.model.user;

/** Creates volunteer accounts. */
public final class VolunteerFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.VOLUNTEER;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new Volunteer(name, email, contactInfo);
    }
}

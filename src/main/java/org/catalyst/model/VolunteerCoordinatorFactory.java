package org.catalyst.model;

/** Creates volunteer coordinator accounts. */
public final class VolunteerCoordinatorFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.VOLUNTEER_COORDINATOR;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new VolunteerCoordinator(name, email, contactInfo);
    }
}

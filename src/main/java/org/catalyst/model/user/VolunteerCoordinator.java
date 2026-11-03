package org.catalyst.model.user;

/** Staff account responsible for coordinating volunteers. */
public final class VolunteerCoordinator extends User {
    public VolunteerCoordinator(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.VOLUNTEER_COORDINATOR;
    }
}

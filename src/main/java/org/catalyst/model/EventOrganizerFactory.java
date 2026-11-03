package org.catalyst.model;

/** Creates event organizer accounts. */
public final class EventOrganizerFactory extends UserFactory {
    @Override
    public UserType getUserType() {
        return UserType.EVENT_ORGANIZER;
    }

    @Override
    public User createUser(
            String name, String email, String contactInfo, String organization) {
        return new EventOrganizer(name, email, contactInfo);
    }
}

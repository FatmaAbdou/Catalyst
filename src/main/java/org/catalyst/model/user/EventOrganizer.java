package org.catalyst.model.user;

/** Staff account responsible for organizing events. */
public final class EventOrganizer extends User {
    public EventOrganizer(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.EVENT_ORGANIZER;
    }
}

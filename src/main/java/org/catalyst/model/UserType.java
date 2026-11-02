package org.catalyst.model;

/** Account roles represented in the class diagram and database. */
public enum UserType {
    DONOR,
    VOLUNTEER,
    DONATION_OFFICER,
    VOLUNTEER_COORDINATOR,
    EVENT_ORGANIZER,
    BENEFICIARY_OFFICER,
    ADMINISTRATOR;

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

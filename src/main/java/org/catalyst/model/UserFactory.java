package org.catalyst.model;

/** Creates the user subtype selected on the sign-up page. */
public final class UserFactory {
    private UserFactory() {
    }

    public static User createUser(
            UserType type, String name, String email, String contactInfo, String organization) {
        return switch (type) {
            case DONOR -> new Donor(name, email, contactInfo, organization);
            case VOLUNTEER -> new Volunteer(name, email, contactInfo);
        };
    }
}

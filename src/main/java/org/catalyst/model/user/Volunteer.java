package org.catalyst.model.user;

/** A volunteer account, with preferences collected later in the volunteer workflow. */
public final class Volunteer extends User {

    public Volunteer(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.VOLUNTEER;
    }
}

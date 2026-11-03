package org.catalyst.model.user;

/** Staff account with administrative responsibilities. */
public final class Administrator extends User {
    public Administrator(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    @Override
    public UserType getUserType() {
        return UserType.ADMINISTRATOR;
    }
}

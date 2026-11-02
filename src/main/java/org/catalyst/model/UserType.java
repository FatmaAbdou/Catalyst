package org.catalyst.model;

/** Account types that a new user can choose during sign-up. */
public enum UserType {
    DONOR,
    VOLUNTEER;

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

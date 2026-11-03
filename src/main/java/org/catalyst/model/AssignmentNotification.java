package org.catalyst.model;

import java.time.LocalDateTime;

/** An in-app notice linked to the volunteer task it is about. */
public record AssignmentNotification(
        int id,
        String message,
        LocalDateTime sentAt,
        boolean read,
        VolunteerTask task) {

    @Override
    public String toString() {
        return (read ? "" : "●  ") + message + " · " + sentAt.toLocalDate();
    }
}

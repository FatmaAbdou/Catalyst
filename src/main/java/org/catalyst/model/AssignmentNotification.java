package org.catalyst.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.catalyst.model.volunteer.VolunteerTask;

/** An in-app notice linked to the volunteer task it is about. */
public final class AssignmentNotification {
    private final int id;
    private final String message;
    private final LocalDateTime sentAt;
    private final boolean read;
    private final VolunteerTask task;

    public AssignmentNotification(
            int id, String message, LocalDateTime sentAt, boolean read, VolunteerTask task) {
        this.id = id;
        this.message = message;
        this.sentAt = sentAt;
        this.read = read;
        this.task = task;
    }

    public int id() {
        return id;
    }

    public String message() {
        return message;
    }

    public LocalDateTime sentAt() {
        return sentAt;
    }

    public boolean read() {
        return read;
    }

    public VolunteerTask task() {
        return task;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentNotification notification)) {
            return false;
        }
        return id == notification.id
                && read == notification.read
                && Objects.equals(message, notification.message)
                && Objects.equals(sentAt, notification.sentAt)
                && Objects.equals(task, notification.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, sentAt, read, task);
    }

    @Override
    public String toString() {
        return (read ? "" : "●  ") + message + " · " + sentAt.toLocalDate();
    }
}

package org.catalyst.model;

import java.time.LocalDateTime;

/** A task assigned to a volunteer. */
public record VolunteerAssignment(
        int id, String description, LocalDateTime startsAt, LocalDateTime endsAt, String status) {

    @Override
    public String toString() {
        return description + "  ·  " + startsAt + "  ·  " + status;
    }
}

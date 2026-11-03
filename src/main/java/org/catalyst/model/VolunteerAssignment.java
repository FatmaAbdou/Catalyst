package org.catalyst.model;

import java.time.LocalDateTime;

/** A volunteer assigned to a task. */
public record VolunteerAssignment(
        int id,
        int taskId,
        int userId,
        String volunteerName,
        String taskDescription,
        LocalDateTime assignedAt,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status) {

    @Override
    public String toString() {
        return volunteerName + " — " + taskDescription + " (" + status + ")";
    }
}

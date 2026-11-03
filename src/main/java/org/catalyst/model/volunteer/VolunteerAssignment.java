package org.catalyst.model.volunteer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.catalyst.model.ServiceHour;

/** A volunteer assigned to a task. */
public final class VolunteerAssignment {
    private final int id;
    private final int taskId;
    private final int userId;
    private final String volunteerName;
    private final String taskDescription;
    private final LocalDateTime assignedAt;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final String status;

    public VolunteerAssignment(
            int id, int taskId, int userId, String volunteerName, String taskDescription,
            LocalDateTime assignedAt, LocalDateTime startsAt, LocalDateTime endsAt, String status) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
        this.volunteerName = volunteerName;
        this.taskDescription = taskDescription;
        this.assignedAt = assignedAt;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public int id() {
        return id;
    }

    public int taskId() {
        return taskId;
    }

    public int userId() {
        return userId;
    }

    public String volunteerName() {
        return volunteerName;
    }

    public String taskDescription() {
        return taskDescription;
    }

    public LocalDateTime assignedAt() {
        return assignedAt;
    }

    public LocalDateTime startsAt() {
        return startsAt;
    }

    public LocalDateTime endsAt() {
        return endsAt;
    }

    public String status() {
        return status;
    }

    /** Creates a pending service-hour entry for this assignment. */
    public ServiceHour logHours(LocalDate date, BigDecimal hours) {
        return new ServiceHour(
                0, id, volunteerName, date, hours, taskDescription, "PENDING");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VolunteerAssignment assignment)) {
            return false;
        }
        return id == assignment.id
                && taskId == assignment.taskId
                && userId == assignment.userId
                && Objects.equals(volunteerName, assignment.volunteerName)
                && Objects.equals(taskDescription, assignment.taskDescription)
                && Objects.equals(assignedAt, assignment.assignedAt)
                && Objects.equals(startsAt, assignment.startsAt)
                && Objects.equals(endsAt, assignment.endsAt)
                && Objects.equals(status, assignment.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, taskId, userId, volunteerName, taskDescription,
                assignedAt, startsAt, endsAt, status);
    }

    @Override
    public String toString() {
        return volunteerName + " — " + taskDescription + " (" + status + ")";
    }
}

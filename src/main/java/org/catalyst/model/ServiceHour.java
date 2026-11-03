package org.catalyst.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Hours recorded against a volunteer assignment. */
public final class ServiceHour {
    private final int id;
    private final int assignmentId;
    private final String volunteerName;
    private final LocalDate date;
    private final BigDecimal hours;
    private final String taskDescription;
    private final String status;

    public ServiceHour(
            int id, int assignmentId, String volunteerName, LocalDate date,
            BigDecimal hours, String taskDescription, String status) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.volunteerName = volunteerName;
        this.date = date;
        this.hours = hours;
        this.taskDescription = taskDescription;
        this.status = status;
    }

    public int id() {
        return id;
    }

    public int assignmentId() {
        return assignmentId;
    }

    public String volunteerName() {
        return volunteerName;
    }

    public LocalDate date() {
        return date;
    }

    public BigDecimal hours() {
        return hours;
    }

    public String taskDescription() {
        return taskDescription;
    }

    public String status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceHour serviceHour)) {
            return false;
        }
        return id == serviceHour.id
                && assignmentId == serviceHour.assignmentId
                && Objects.equals(volunteerName, serviceHour.volunteerName)
                && Objects.equals(date, serviceHour.date)
                && Objects.equals(hours, serviceHour.hours)
                && Objects.equals(taskDescription, serviceHour.taskDescription)
                && Objects.equals(status, serviceHour.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, assignmentId, volunteerName, date, hours, taskDescription, status);
    }

    @Override
    public String toString() {
        String volunteer = volunteerName == null ? "" : volunteerName + " · ";
        return volunteer + date + " · " + hours.stripTrailingZeros().toPlainString()
                + " hours · " + taskDescription + " · " + status;
    }
}

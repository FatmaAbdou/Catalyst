package org.catalyst.model.volunteer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.catalyst.model.Skill;

/** A volunteer task created for an event. */
public final class VolunteerTask {
    private final int id;
    private final Integer eventId;
    private final String eventName;
    private final String description;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final List<Skill> requiredSkills;
    private final int volunteersNum;

    public VolunteerTask(
            int id, Integer eventId, String eventName, String description,
            LocalDateTime startsAt, LocalDateTime endsAt, List<Skill> requiredSkills,
            int volunteersNum) {
        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.requiredSkills = List.copyOf(requiredSkills);
        this.volunteersNum = volunteersNum;
    }

    public int id() {
        return id;
    }

    public Integer eventId() {
        return eventId;
    }

    public String eventName() {
        return eventName;
    }

    public String description() {
        return description;
    }

    public LocalDateTime startsAt() {
        return startsAt;
    }

    public LocalDateTime endsAt() {
        return endsAt;
    }

    public List<Skill> requiredSkills() {
        return requiredSkills;
    }

    public int volunteersNum() {
        return volunteersNum;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VolunteerTask task)) {
            return false;
        }
        return id == task.id
                && volunteersNum == task.volunteersNum
                && Objects.equals(eventId, task.eventId)
                && Objects.equals(eventName, task.eventName)
                && Objects.equals(description, task.description)
                && Objects.equals(startsAt, task.startsAt)
                && Objects.equals(endsAt, task.endsAt)
                && requiredSkills.equals(task.requiredSkills);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, eventId, eventName, description, startsAt, endsAt, requiredSkills, volunteersNum);
    }

    @Override
    public String toString() {
        String event = eventName == null ? "" : " · " + eventName;
        return description + event + " · " + startsAt + " (" + volunteersNum + " volunteers)";
    }
}

package org.catalyst.model;

import java.time.LocalDateTime;
import java.util.List;

/** A volunteer task created for an event. */
public record VolunteerTask(
        int id,
        Integer eventId,
        String eventName,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<String> requiredSkills,
        int volunteersNum) {

    public VolunteerTask {
        requiredSkills = List.copyOf(requiredSkills);
    }

    @Override
    public String toString() {
        String event = eventName == null ? "" : " · " + eventName;
        return description + event + " · " + startsAt + " (" + volunteersNum + " volunteers)";
    }
}

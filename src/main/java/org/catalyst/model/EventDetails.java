package org.catalyst.model;


import java.sql.Time;
import java.util.Date;

/** Input values from the creation form; ID and status are assigned separately. */
public record EventDetails(
        String name,
        String description,
        String location,
        Date date,
        Time startTime,
        Time endTime,
        int capacity,
        float ticketPrice,
        // Workshop fields; unused for fundraising events.
        String instructor,
        String topic,
        String prerequisiteSkills,
        // Fundraising field; unused for workshops.
        float fundraisingGoal) {
}

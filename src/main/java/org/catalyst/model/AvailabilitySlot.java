package org.catalyst.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/** A weekly volunteer availability range. */
public record AvailabilitySlot(DayOfWeek day, LocalTime start, LocalTime end) {
    public AvailabilitySlot {
        Objects.requireNonNull(day, "Choose a day.");
        Objects.requireNonNull(start, "Choose a start time.");
        Objects.requireNonNull(end, "Choose an end time.");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
    }

    @Override
    public String toString() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        return day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                + "  " + start.format(format) + " – " + end.format(format);
    }
}

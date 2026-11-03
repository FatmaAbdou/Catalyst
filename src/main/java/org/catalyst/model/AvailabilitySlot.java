package org.catalyst.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/** A weekly volunteer availability range. */
public final class AvailabilitySlot {
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;

    public AvailabilitySlot(DayOfWeek day, LocalTime start, LocalTime end) {
        this.day = Objects.requireNonNull(day, "Choose a day.");
        this.start = Objects.requireNonNull(start, "Choose a start time.");
        this.end = Objects.requireNonNull(end, "Choose an end time.");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
    }

    public DayOfWeek day() {
        return day;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AvailabilitySlot slot)) {
            return false;
        }
        return day == slot.day && start.equals(slot.start) && end.equals(slot.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, start, end);
    }

    @Override
    public String toString() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        return day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                + "  " + start.format(format) + " – " + end.format(format);
    }
}

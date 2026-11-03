package org.catalyst.model;

import java.util.ArrayList;
import java.util.List;

/** A volunteer account, with preferences collected later in the volunteer workflow. */
public final class Volunteer extends User {
    private List<Skill> skills = new ArrayList<>();
    private List<AvailabilitySlot> availability = new ArrayList<>();
    private String preferences = "";
    private String notificationChannel = "EMAIL";

    public Volunteer(String name, String email, String contactInfo) {
        super(name, email, contactInfo);
    }

    public List<Skill> getSkills() {
        return List.copyOf(skills);
    }

    public void updateSkills(List<Skill> skills) {
        this.skills = new ArrayList<>(skills);
    }

    public List<AvailabilitySlot> getAvailability() {
        return List.copyOf(availability);
    }

    public void updateAvailability(List<AvailabilitySlot> availability) {
        this.availability = new ArrayList<>(availability);
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences == null ? "" : preferences;
    }

    public String getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    @Override
    public UserType getUserType() {
        return UserType.VOLUNTEER;
    }
}

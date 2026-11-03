package org.catalyst.service;

import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerAssignment;
import org.catalyst.repo.VolunteerRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Handles the volunteer profile and activity shown in the volunteer area. */
public final class VolunteerService {
    private final VolunteerRepository repository;

    public VolunteerService(VolunteerRepository repository) {
        this.repository = repository;
    }

    public void load(Volunteer volunteer) throws SQLException {
        repository.loadProfile(volunteer);
    }

    public List<String> getSkills() throws SQLException {
        return repository.getSkills();
    }

    public String addSkill(String skill) throws SQLException {
        String cleanSkill = skill == null ? "" : skill.trim();
        if (cleanSkill.isBlank() || cleanSkill.length() > 120) {
            throw new IllegalArgumentException("A skill must be 1 to 120 characters.");
        }
        for (String savedSkill : repository.getSkills()) {
            if (savedSkill.equalsIgnoreCase(cleanSkill)) {
                return savedSkill;
            }
        }
        return repository.addSkill(cleanSkill);
    }

    public void save(
            Volunteer volunteer, Set<String> skills, List<AvailabilitySlot> availability,
            String preferences, String notificationChannel) throws SQLException {
        List<String> cleanSkills = cleanSkills(skills);
        if (!"EMAIL".equals(notificationChannel) && !"SMS".equals(notificationChannel)) {
            throw new IllegalArgumentException("Choose EMAIL or SMS for notifications.");
        }

        volunteer.updateSkills(cleanSkills);
        volunteer.updateAvailability(availability);
        volunteer.setPreferences(preferences == null ? "" : preferences.trim());
        volunteer.setNotificationChannel(notificationChannel);
        repository.saveProfile(volunteer);
    }

    public List<VolunteerAssignment> getAssignments(Volunteer volunteer) throws SQLException {
        return repository.getAssignments(volunteer.getId());
    }

    public List<ServiceHour> getServiceHours(Volunteer volunteer) throws SQLException {
        return repository.getServiceHours(volunteer.getId());
    }

    private List<String> cleanSkills(Set<String> skills) {
        Set<String> cleanSkills = new LinkedHashSet<>();
        for (String skill : skills) {
            String cleanSkill = skill.trim();
            if (cleanSkill.isBlank() || cleanSkill.length() > 120) {
                throw new IllegalArgumentException(
                        "A skill must be 1 to 120 characters.");
            }
            cleanSkills.add(cleanSkill);
        }
        return new ArrayList<>(cleanSkills);
    }
}

package org.catalyst.service;

import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.AssignmentNotification;
import org.catalyst.model.Certificate;
import org.catalyst.model.CertificateFactory;
import org.catalyst.model.AppreciationCertificateFactory;
import org.catalyst.model.HoursMilestoneCertificateFactory;
import org.catalyst.model.ParticipationCertificateFactory;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Skill;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerAssignment;
import org.catalyst.model.VolunteerTask;
import org.catalyst.repo.VolunteerRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handles volunteer profiles, tasks, assignments, and service hours. */
public final class VolunteerService {
    private final VolunteerRepository repository;

    public VolunteerService(VolunteerRepository repository) {
        this.repository = repository;
    }

    public void load(Volunteer volunteer) throws SQLException {
        repository.loadProfile(volunteer);
    }

    public List<Skill> getSkills() throws SQLException {
        return repository.getSkills();
    }

    public Skill addSkill(String skill) throws SQLException {
        String cleanSkill = skill == null ? "" : skill.trim();
        if (cleanSkill.isBlank() || cleanSkill.length() > 120) {
            throw new IllegalArgumentException("A skill must be 1 to 120 characters.");
        }
        for (Skill savedSkill : repository.getSkills()) {
            if (savedSkill.name().equalsIgnoreCase(cleanSkill)) {
                return savedSkill;
            }
        }
        return repository.addSkill(cleanSkill);
    }

    public void save(
            Volunteer volunteer, List<Skill> skills, List<AvailabilitySlot> availability,
            String preferences, String notificationChannel) throws SQLException {
        List<Skill> cleanSkills = cleanVolunteerSkills(skills);
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

    public List<AssignmentNotification> getAssignmentNotifications(Volunteer volunteer)
            throws SQLException {
        return repository.getAssignmentNotifications(volunteer.getId());
    }

    public int getUnreadAssignmentNotificationCount(Volunteer volunteer) throws SQLException {
        return repository.getUnreadAssignmentNotificationCount(volunteer.getId());
    }

    public void markAssignmentNotificationRead(
            Volunteer volunteer, AssignmentNotification notification) throws SQLException {
        if (notification == null) {
            throw new IllegalArgumentException("Select a notification first.");
        }
        repository.markAssignmentNotificationRead(volunteer.getId(), notification.id());
    }

    public List<VolunteerAssignment> getScheduleAssignments() throws SQLException {
        return repository.getAllAssignments();
    }

    public List<VolunteerAssignment> getScheduleAssignments(Volunteer volunteer)
            throws SQLException {
        return repository.getAssignments(volunteer.getId());
    }

    public int getApprovedHours(Volunteer volunteer) throws SQLException {
        return repository.getApprovedHours(volunteer.getId());
    }

    public boolean isAvailableForTask(Volunteer volunteer, VolunteerTask task) {
        if (volunteer == null || task == null
                || !task.startsAt().toLocalDate().equals(task.endsAt().toLocalDate())) {
            return false;
        }
        LocalTime startsAt = task.startsAt().toLocalTime();
        LocalTime endsAt = task.endsAt().toLocalTime();
        return volunteer.getAvailability().stream().anyMatch(slot ->
                slot.day() == task.startsAt().getDayOfWeek()
                        && !slot.start().isAfter(startsAt)
                        && !slot.end().isBefore(endsAt));
    }

    public void recordHours(
            Volunteer volunteer, VolunteerAssignment assignment, LocalDate date, BigDecimal hours)
            throws SQLException {
        validateHours(date, hours);
        if (assignment == null) {
            throw new IllegalArgumentException("Select an active assignment.");
        }
        ServiceHour serviceHour = assignment.logHours(date, hours);
        repository.recordHours(volunteer.getId(), serviceHour);
    }

    public List<ServiceHour> getPendingServiceHours() throws SQLException {
        return repository.getPendingServiceHours();
    }

    public void approveServiceHour(ServiceHour serviceHour) throws SQLException {
        if (serviceHour == null) {
            throw new IllegalArgumentException("Select service hours to approve.");
        }
        repository.approveServiceHour(serviceHour.id());
    }

    public List<Volunteer> searchVolunteers(String filter) throws SQLException {
        String cleanFilter = filter == null ? "" : filter.trim();
        if (cleanFilter.length() > 255) {
            throw new IllegalArgumentException("Search text must be 255 characters or fewer.");
        }
        return repository.searchVolunteers(cleanFilter);
    }

    public List<VolunteerTask> getTasks() throws SQLException {
        return repository.getTasks();
    }

    public void deleteTask(VolunteerTask task) throws SQLException {
        if (task == null) {
            throw new IllegalArgumentException("Select a task to delete.");
        }
        repository.deleteTask(task.id());
    }

    public Map<Integer, String> getEvents() throws SQLException {
        return repository.getEvents();
    }

    public void createTask(
            Integer eventId, String description, LocalDateTime startsAt, LocalDateTime endsAt,
            Set<Skill> requiredSkills, int volunteersNum) throws SQLException {
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.isBlank() || cleanDescription.length() > 2000) {
            throw new IllegalArgumentException("Task description must be 1 to 2000 characters.");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Task end time must be after its start time.");
        }
        if (volunteersNum < 1) {
            throw new IllegalArgumentException("The volunteer limit must be at least one.");
        }
        if (eventId != null && !repository.getEvents().containsKey(eventId)) {
            throw new IllegalArgumentException("Choose a currently upcoming event, or leave it blank.");
        }
        List<Skill> cleanSkills = cleanRequiredSkills(requiredSkills, repository.getSkills());
        repository.createTask(new VolunteerTask(
                0, eventId, null, cleanDescription, startsAt, endsAt, cleanSkills, volunteersNum));
    }

    public List<VolunteerAssignment> getAssignments(VolunteerTask task) throws SQLException {
        return repository.getAssignmentsForTask(task.id());
    }

    public List<VolunteerTask> getCertificateTasks(Volunteer volunteer) throws SQLException {
        return repository.getCertificateTasks(volunteer.getId());
    }

    public Certificate createCertificate(
            String type, Volunteer volunteer, VolunteerTask task, String message)
            throws SQLException {
        if (volunteer == null) {
            throw new IllegalArgumentException("Select a volunteer.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Choose a certificate type.");
        }
        CertificateFactory factory = switch (type) {
            case "Participation" -> {
                boolean participated = task != null && getCertificateTasks(volunteer).stream()
                        .anyMatch(approvedTask -> approvedTask.id() == task.id());
                if (!participated) {
                    throw new IllegalArgumentException(
                            "Participation certificates require approved service hours for that task.");
                }
                yield new ParticipationCertificateFactory();
            }
            case "Hours milestone" -> new HoursMilestoneCertificateFactory();
            case "Appreciation" -> new AppreciationCertificateFactory();
            default -> throw new IllegalArgumentException("Choose a certificate type.");
        };
        int approvedHours = repository.getApprovedHours(volunteer.getId());
        return factory.create(volunteer, task, approvedHours, message);
    }

    private void validateHours(LocalDate date, BigDecimal hours) {
        if (date == null || date.isAfter(LocalDate.now()) || hours == null || hours.signum() <= 0
                || hours.stripTrailingZeros().scale() > 2
                || hours.compareTo(new BigDecimal("9999.99")) > 0) {
            throw new IllegalArgumentException(
                    "Enter today or an earlier date and positive hours (up to 2 decimals).");
        }
    }

    private List<Skill> cleanVolunteerSkills(List<Skill> skills) throws SQLException {
        Map<Integer, Skill> catalogById = repository.getSkills().stream()
                .collect(java.util.stream.Collectors.toMap(Skill::id, skill -> skill));
        Map<Integer, Skill> cleanSkills = new java.util.LinkedHashMap<>();
        for (Skill skill : skills) {
            Skill catalogSkill = catalogById.get(skill.id());
            if (catalogSkill == null || !catalogSkill.name().equals(skill.name())) {
                throw new IllegalArgumentException("Choose skills from the shared skill list.");
            }
            if (!Skill.LEVELS.contains(skill.level())) {
                throw new IllegalArgumentException("Choose a valid level for each skill.");
            }
            cleanSkills.put(skill.id(),
                    new Skill(skill.id(), catalogSkill.name(), skill.level()));
        }
        return new ArrayList<>(cleanSkills.values());
    }

    private List<Skill> cleanRequiredSkills(Set<Skill> skills, List<Skill> catalog) {
        Map<Integer, Skill> catalogById = catalog.stream()
                .collect(java.util.stream.Collectors.toMap(Skill::id, skill -> skill));
        Map<Integer, Skill> cleanSkills = new java.util.LinkedHashMap<>();
        for (Skill skill : skills) {
            Skill catalogSkill = catalogById.get(skill.id());
            if (catalogSkill == null || !catalogSkill.name().equals(skill.name())) {
                throw new IllegalArgumentException("Choose skills from the shared skill list.");
            }
            cleanSkills.put(skill.id(),
                    new Skill(skill.id(), catalogSkill.name(), Skill.ANY_LEVEL));
        }
        return new ArrayList<>(cleanSkills.values());
    }
}

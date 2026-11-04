package org.catalyst.repo;

import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.AssignmentNotification;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Skill;
import org.catalyst.model.volunteer.Volunteer;
import org.catalyst.model.volunteer.VolunteerAssignment;
import org.catalyst.model.volunteer.VolunteerTask;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads and updates a volunteer profile and its activity. */
public final class VolunteerRepository {

    public void loadProfile(Volunteer volunteer) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        String sql = """
                SELECT vp.volunteer_id, vp.preferences, u.preferred_channel
                FROM volunteer_profiles vp
                JOIN users u ON u.user_id = vp.user_id
                WHERE vp.user_id = ?
                """;
        int volunteerId;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, volunteer.getId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Volunteer profile was not found.");
                }
                volunteerId = result.getInt("volunteer_id");
                String preferences = result.getString("preferences");
                volunteer.setPreferences(preferences == null ? "" : preferences);
                String channel = result.getString("preferred_channel");
                volunteer.setNotificationChannel(channel == null ? "EMAIL" : channel);
            }
        }

        volunteer.updateSkills(readSkills(connection, """
                SELECT s.skill_id, s.skill_name, vs.skill_level
                FROM skills s
                JOIN volunteer_skills vs ON vs.skill_id = s.skill_id
                WHERE vs.volunteer_id = ?
                ORDER BY s.skill_name
                """, volunteerId));
        volunteer.updateAvailability(readAvailability(connection, """
                SELECT day_of_week, start_time, end_time
                FROM volunteer_availability
                WHERE volunteer_id = ? AND day_of_week IS NOT NULL
                ORDER BY day_of_week, start_time
                """, volunteerId));
    }

    public List<Skill> getSkills() throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT skill_id, skill_name FROM skills ORDER BY skill_name";
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                skills.add(new Skill(result.getInt("skill_id"),
                        result.getString("skill_name"), Skill.ANY_LEVEL));
            }
        }
        return skills;
    }

    public Map<Integer, String> getEvents() throws SQLException {
        Map<Integer, String> events = new LinkedHashMap<>();
        String sql = """
                SELECT event_id, name
                FROM events
                WHERE status = 'UPCOMING'
                ORDER BY starts_at
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                events.put(result.getInt("event_id"), result.getString("name"));
            }
        }
        return events;
    }

    public List<VolunteerTask> getTasks() throws SQLException {
        String sql = """
                SELECT t.task_id, t.event_id, e.name AS event_name, t.description,
                       t.starts_at, t.ends_at, t.volunteers_num
                FROM volunteer_tasks t
                LEFT JOIN events e ON e.event_id = t.event_id
                ORDER BY t.starts_at
                """;
        List<VolunteerTask> tasks = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int taskId = result.getInt("task_id");
                tasks.add(new VolunteerTask(
                        taskId,
                        (Integer) result.getObject("event_id"),
                        result.getString("event_name"),
                        result.getString("description"),
                        result.getTimestamp("starts_at").toLocalDateTime(),
                        result.getTimestamp("ends_at").toLocalDateTime(),
                        getTaskSkills(connection, taskId),
                        result.getInt("volunteers_num")));
            }
        }
        return tasks;
    }

    public void createTask(VolunteerTask task) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int taskId;
            String taskSql = """
                    INSERT INTO volunteer_tasks
                        (event_id, description, starts_at, ends_at, volunteers_num)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING task_id
                    """;
            try (PreparedStatement statement = connection.prepareStatement(taskSql)) {
                if (task.eventId() == null) {
                    statement.setNull(1, Types.INTEGER);
                } else {
                    statement.setInt(1, task.eventId());
                }
                statement.setString(2, task.description());
                statement.setTimestamp(3, Timestamp.valueOf(task.startsAt()));
                statement.setTimestamp(4, Timestamp.valueOf(task.endsAt()));
                statement.setInt(5, task.volunteersNum());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("The task could not be created.");
                    }
                    taskId = result.getInt(1);
                }
            }

            String skillSql = """
                    INSERT INTO task_required_skills (task_id, skill_id)
                    SELECT ?, skill_id FROM skills WHERE skill_name = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(skillSql)) {
                for (Skill skill : task.requiredSkills()) {
                    statement.setInt(1, taskId);
                    statement.setString(2, skill.name());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("A required skill was not found: " + skill.name());
                    }
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public void deleteTask(int taskId) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String historySql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM service_records sr
                        JOIN volunteer_assignments a ON a.assignment_id = sr.assignment_id
                        WHERE a.task_id = ?
                    )
                    """;
            try (PreparedStatement statement = connection.prepareStatement(historySql)) {
                statement.setInt(1, taskId);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    if (result.getBoolean(1)) {
                        throw new IllegalArgumentException(
                                "This task has submitted service hours and must be kept for history.");
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM volunteer_assignments WHERE task_id = ?")) {
                statement.setInt(1, taskId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM task_required_skills WHERE task_id = ?")) {
                statement.setInt(1, taskId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM volunteer_tasks WHERE task_id = ?")) {
                statement.setInt(1, taskId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("The selected task no longer exists.");
                }
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public List<Volunteer> searchVolunteers(String filter) throws SQLException {
        String sql = """
                SELECT u.user_id, vp.volunteer_id, u.name, u.email, u.contact_info,
                       vp.preferences, s.skill_id, s.skill_name, vs.skill_level
                FROM users u
                JOIN volunteer_profiles vp ON vp.user_id = u.user_id
                LEFT JOIN volunteer_skills vs ON vs.volunteer_id = vp.volunteer_id
                LEFT JOIN skills s ON s.skill_id = vs.skill_id
                WHERE u.status = 'ACTIVE'
                  AND (? = '' OR u.name ILIKE ?)
                ORDER BY u.name, s.skill_name
                """;
        String cleanFilter = filter == null ? "" : filter.trim();
        String query = "%" + cleanFilter + "%";
        Map<Integer, Volunteer> volunteers = new LinkedHashMap<>();
        Map<Integer, List<Skill>> skillsByVolunteer = new LinkedHashMap<>();
        Map<Integer, Integer> volunteerIds = new LinkedHashMap<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cleanFilter);
            statement.setString(2, query);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int userId = result.getInt("user_id");
                    Volunteer volunteer = volunteers.get(userId);
                    if (volunteer == null) {
                        volunteer = new Volunteer(
                                result.getString("name"),
                                result.getString("email"),
                                result.getString("contact_info"));
                        volunteer.setId(userId);
                        volunteer.setPreferences(result.getString("preferences"));
                        volunteers.put(userId, volunteer);
                        volunteerIds.put(userId, result.getInt("volunteer_id"));
                    }
                    skillsByVolunteer.computeIfAbsent(userId, id -> new ArrayList<>());
                    String skillName = result.getString("skill_name");
                    if (skillName != null) {
                        skillsByVolunteer.get(userId).add(new Skill(
                                result.getInt("skill_id"),
                                skillName,
                                result.getString("skill_level")));
                    }
                }
            }
        }
        for (Map.Entry<Integer, Volunteer> entry : volunteers.entrySet()) {
            entry.getValue().updateSkills(skillsByVolunteer.get(entry.getKey()));
            entry.getValue().updateAvailability(readAvailability(
                    connection,
                    """
                            SELECT day_of_week, start_time, end_time
                            FROM volunteer_availability
                            WHERE volunteer_id = ? AND day_of_week IS NOT NULL
                            ORDER BY day_of_week, start_time
                            """,
                    volunteerIds.get(entry.getKey())));
        }
        return new ArrayList<>(volunteers.values());
    }

    public List<VolunteerAssignment> getAssignmentsForTask(int taskId) throws SQLException {
        String sql = """
                SELECT a.assignment_id, a.task_id, vp.user_id, u.name,
                       a.assigned_at, t.description, t.starts_at, t.ends_at, a.status
                FROM volunteer_assignments a
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE a.task_id = ? AND a.status = 'ASSIGNED'
                ORDER BY u.name
                """;
        List<VolunteerAssignment> assignments = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    assignments.add(readAssignment(result));
                }
            }
        }
        loadServiceHours(connection, assignments);
        return assignments;
    }

    public List<VolunteerAssignment> getAllAssignments() throws SQLException {
        String sql = """
                SELECT a.assignment_id, a.task_id, vp.user_id, u.name,
                       a.assigned_at, t.description, t.starts_at, t.ends_at, a.status
                FROM volunteer_assignments a
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE a.status = 'ASSIGNED'
                ORDER BY t.starts_at, u.name
                """;
        List<VolunteerAssignment> assignments = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                assignments.add(readAssignment(result));
            }
        }
        loadServiceHours(connection, assignments);
        return assignments;
    }

    public VolunteerAssignment assign(int userId, int taskId) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int capacity;
            LocalDateTime startsAt;
            LocalDateTime endsAt;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT volunteers_num, starts_at, ends_at FROM volunteer_tasks "
                            + "WHERE task_id = ? FOR UPDATE")) {
                statement.setInt(1, taskId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Select a task first.");
                    }
                    capacity = result.getInt("volunteers_num");
                    startsAt = result.getTimestamp("starts_at").toLocalDateTime();
                    endsAt = result.getTimestamp("ends_at").toLocalDateTime();
                }
            }

            int volunteerId = findVolunteerId(connection, userId);
            if (!isAvailable(connection, volunteerId, startsAt, endsAt)) {
                throw new IllegalArgumentException(
                        "This volunteer is not available during the task schedule.");
            }
            int assignedCount;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM volunteer_assignments "
                            + "WHERE task_id = ? AND status = 'ASSIGNED'")) {
                statement.setInt(1, taskId);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    assignedCount = result.getInt(1);
                }
            }
            boolean alreadyAssigned = isAlreadyAssigned(connection, taskId, volunteerId);
            if (!alreadyAssigned && assignedCount >= capacity) {
                throw new IllegalArgumentException("This task has reached its volunteer limit.");
            }

            String sql = """
                    INSERT INTO volunteer_assignments (task_id, volunteer_id)
                    VALUES (?, ?)
                    ON CONFLICT (task_id, volunteer_id) DO UPDATE
                    SET status = 'ASSIGNED', assigned_at = CURRENT_TIMESTAMP
                    WHERE volunteer_assignments.status <> 'ASSIGNED'
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, taskId);
                statement.setInt(2, volunteerId);
                int changed = statement.executeUpdate();
                if (changed == 0) {
                    throw new IllegalArgumentException("This volunteer is already assigned.");
                }
            }
            connection.commit();
            return getAssignment(connection, taskId, volunteerId);
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public void unassign(int assignmentId) throws SQLException {
        String sql = """
                UPDATE volunteer_assignments
                SET status = 'UNASSIGNED'
                WHERE assignment_id = ? AND status = 'ASSIGNED'
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("That assignment is no longer active.");
            }
        }
    }

    public void deleteAssignment(int assignmentId) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        String historySql = """
                SELECT EXISTS (
                    SELECT 1 FROM service_records WHERE assignment_id = ?
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(historySql)) {
            statement.setInt(1, assignmentId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                if (result.getBoolean(1)) {
                    throw new IllegalArgumentException(
                            "This assignment has service-hour history. Unassign it instead.");
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM volunteer_assignments WHERE assignment_id = ?")) {
            statement.setInt(1, assignmentId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("The selected assignment no longer exists.");
            }
        }
    }

    public void recordHours(int userId, ServiceHour serviceHour)
            throws SQLException {
        int assignmentId = serviceHour.assignmentId();
        LocalDate date = serviceHour.date();
        BigDecimal hours = serviceHour.hours();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            LocalDateTime startsAt;
            LocalDateTime endsAt;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT t.starts_at, t.ends_at
                    FROM volunteer_assignments a
                    JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                    JOIN volunteer_tasks t ON t.task_id = a.task_id
                    WHERE a.assignment_id = ? AND vp.user_id = ? AND a.status = 'ASSIGNED'
                    FOR UPDATE OF a
                    """)) {
                statement.setInt(1, assignmentId);
                statement.setInt(2, userId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Select one of your active assignments.");
                    }
                    startsAt = result.getTimestamp("starts_at").toLocalDateTime();
                    endsAt = result.getTimestamp("ends_at").toLocalDateTime();
                }
            }

            if (date.isBefore(startsAt.toLocalDate()) || date.isAfter(endsAt.toLocalDate())) {
                throw new IllegalArgumentException(
                        "Choose a service date within the task schedule.");
            }

            BigDecimal alreadyLogged;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(SUM(hours), 0) FROM service_records WHERE assignment_id = ?")) {
                statement.setInt(1, assignmentId);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    alreadyLogged = result.getBigDecimal(1);
                }
            }
            BigDecimal scheduledHours = BigDecimal.valueOf(
                    java.time.Duration.between(startsAt, endsAt).toSeconds())
                    .divide(BigDecimal.valueOf(3600), 2, java.math.RoundingMode.DOWN);
            if (alreadyLogged.add(hours).compareTo(scheduledHours) > 0) {
                throw new IllegalArgumentException(
                        "Total logged hours cannot exceed the task's scheduled duration of "
                                + scheduledHours.stripTrailingZeros().toPlainString() + " hours.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO service_records (assignment_id, service_date, hours) "
                            + "VALUES (?, ?, ?)")) {
                statement.setInt(1, assignmentId);
                statement.setDate(2, Date.valueOf(date));
                statement.setBigDecimal(3, hours);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private boolean isAvailable(
            Connection connection, int volunteerId, LocalDateTime startsAt, LocalDateTime endsAt)
            throws SQLException {
        if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            return false;
        }
        LocalTime taskStart = startsAt.toLocalTime();
        LocalTime taskEnd = endsAt.toLocalTime();
        return readAvailability(connection, """
                SELECT day_of_week, start_time, end_time
                FROM volunteer_availability
                WHERE volunteer_id = ? AND day_of_week IS NOT NULL
                """, volunteerId).stream().anyMatch(slot ->
                slot.day() == startsAt.getDayOfWeek()
                        && !slot.start().isAfter(taskStart)
                        && !slot.end().isBefore(taskEnd));
    }

    public List<ServiceHour> getPendingServiceHours() throws SQLException {
        String sql = """
                SELECT sr.service_record_id, sr.assignment_id, u.name AS volunteer_name,
                       sr.service_date, sr.hours, t.description, sr.status
                FROM service_records sr
                JOIN volunteer_assignments a ON a.assignment_id = sr.assignment_id
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE sr.status = 'PENDING'
                ORDER BY sr.service_date, u.name
                """;
        List<ServiceHour> hours = new ArrayList<>();
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                hours.add(readServiceHour(result));
            }
        }
        return hours;
    }

    public void approveServiceHour(int serviceRecordId) throws SQLException {
        String sql = """
                UPDATE service_records
                SET status = 'APPROVED'
                WHERE service_record_id = ? AND status = 'PENDING'
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, serviceRecordId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("That service-hour entry is no longer pending.");
            }
        }
    }

    public int getApprovedHours(int userId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(sr.hours), 0)
                FROM service_records sr
                JOIN volunteer_assignments a ON a.assignment_id = sr.assignment_id
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                WHERE vp.user_id = ? AND sr.status = 'APPROVED'
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBigDecimal(1).intValue();
            }
        }
    }

    public List<VolunteerTask> getCertificateTasks(int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT t.task_id, t.event_id, e.name AS event_name, t.description,
                       t.starts_at, t.ends_at, t.volunteers_num
                FROM volunteer_tasks t
                LEFT JOIN events e ON e.event_id = t.event_id
                JOIN volunteer_assignments a ON a.task_id = t.task_id
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN service_records sr ON sr.assignment_id = a.assignment_id
                WHERE vp.user_id = ? AND sr.status = 'APPROVED'
                ORDER BY t.starts_at
                """;
        List<VolunteerTask> tasks = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int taskId = result.getInt("task_id");
                    tasks.add(new VolunteerTask(
                            taskId,
                            (Integer) result.getObject("event_id"),
                            result.getString("event_name"),
                            result.getString("description"),
                            result.getTimestamp("starts_at").toLocalDateTime(),
                            result.getTimestamp("ends_at").toLocalDateTime(),
                            getTaskSkills(connection, taskId),
                            result.getInt("volunteers_num")));
                }
            }
        }
        return tasks;
    }

    public void queueAssignmentNotification(int userId, int assignmentId, String message)
            throws SQLException {
        String sql = """
                INSERT INTO communications
                    (recipient_user_id, channel, message_content, sending_status, assignment_id)
                SELECT user_id, COALESCE(preferred_channel, 'EMAIL'), ?, 'PENDING', ?
                FROM users
                WHERE user_id = ?
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setString(1, message);
            statement.setInt(2, assignmentId);
            statement.setInt(3, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The volunteer assignment notification could not be queued.");
            }
        }
    }

    public List<AssignmentNotification> getAssignmentNotifications(int userId) throws SQLException {
        String sql = """
                SELECT c.communication_id, c.message_content, c.sent_at, c.read_at,
                       t.task_id, t.event_id, e.name AS event_name, t.description,
                       t.starts_at, t.ends_at, t.volunteers_num
                FROM communications c
                JOIN volunteer_assignments a ON a.assignment_id = c.assignment_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                LEFT JOIN events e ON e.event_id = t.event_id
                WHERE c.recipient_user_id = ? AND c.assignment_id IS NOT NULL
                ORDER BY sent_at DESC
                """;
        List<AssignmentNotification> notifications = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int taskId = result.getInt("task_id");
                    VolunteerTask task = new VolunteerTask(
                            taskId,
                            (Integer) result.getObject("event_id"),
                            result.getString("event_name"),
                            result.getString("description"),
                            result.getTimestamp("starts_at").toLocalDateTime(),
                            result.getTimestamp("ends_at").toLocalDateTime(),
                            getTaskSkills(connection, taskId),
                            result.getInt("volunteers_num"));
                    notifications.add(new AssignmentNotification(
                            result.getInt("communication_id"),
                            result.getString("message_content"),
                            result.getTimestamp("sent_at").toLocalDateTime(),
                            result.getTimestamp("read_at") != null,
                            task));
                }
            }
        }
        return notifications;
    }

    public int getUnreadAssignmentNotificationCount(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM communications
                WHERE recipient_user_id = ? AND assignment_id IS NOT NULL AND read_at IS NULL
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public void markAssignmentNotificationRead(int userId, int notificationId)
            throws SQLException {
        String sql = """
                UPDATE communications
                SET read_at = CURRENT_TIMESTAMP
                WHERE communication_id = ? AND recipient_user_id = ?
                  AND assignment_id IS NOT NULL AND read_at IS NULL
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public Skill addSkill(String name) throws SQLException {
        String sql = """
                INSERT INTO skills (skill_name) VALUES (?)
                ON CONFLICT (skill_name) DO UPDATE SET skill_name = EXCLUDED.skill_name
                RETURNING skill_id, skill_name
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The skill could not be saved.");
                }
                return new Skill(
                        result.getInt("skill_id"),
                        result.getString("skill_name"),
                        Skill.ANY_LEVEL);
            }
        }
    }

    public void saveProfile(Volunteer volunteer) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int volunteerId = findVolunteerId(connection, volunteer.getId());

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE volunteer_profiles SET preferences = ? WHERE volunteer_id = ?")) {
                statement.setString(1, volunteer.getPreferences());
                statement.setInt(2, volunteerId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET preferred_channel = ? WHERE user_id = ?")) {
                statement.setString(1, volunteer.getNotificationChannel());
                statement.setInt(2, volunteer.getId());
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM volunteer_skills WHERE volunteer_id = ?")) {
                statement.setInt(1, volunteerId);
                statement.executeUpdate();
            }
            for (Skill skill : volunteer.getSkills()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        """
                                INSERT INTO volunteer_skills
                                    (volunteer_id, skill_id, skill_level)
                                VALUES (?, ?, ?)
                                """)) {
                    statement.setInt(1, volunteerId);
                    statement.setInt(2, skill.id());
                    statement.setString(3, skill.level());
                    statement.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM volunteer_availability WHERE volunteer_id = ?")) {
                statement.setInt(1, volunteerId);
                statement.executeUpdate();
            }
            String availabilitySql = """
                    INSERT INTO volunteer_availability
                        (volunteer_id, availability_details, day_of_week, start_time, end_time)
                    VALUES (?, NULL, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(availabilitySql)) {
                for (AvailabilitySlot slot : volunteer.getAvailability()) {
                    statement.setInt(1, volunteerId);
                    statement.setString(2, slot.day().name());
                    statement.setObject(3, slot.start());
                    statement.setObject(4, slot.end());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public List<VolunteerAssignment> getAssignments(int userId) throws SQLException {
        String sql = """
                SELECT a.assignment_id, a.task_id, vp.user_id, u.name,
                       a.assigned_at, t.description, t.starts_at, t.ends_at, a.status
                FROM volunteer_assignments a
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE vp.user_id = ? AND a.status = 'ASSIGNED'
                ORDER BY t.starts_at
                """;
        List<VolunteerAssignment> assignments = new ArrayList<>();
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    assignments.add(readAssignment(result));
                }
            }
        }
        loadServiceHours(connection, assignments);
        return assignments;
    }

    public List<ServiceHour> getServiceHours(int userId) throws SQLException {
        String sql = """
                SELECT sr.service_record_id, sr.assignment_id, NULL AS volunteer_name, sr.service_date,
                       sr.hours, t.description, sr.status
                FROM service_records sr
                JOIN volunteer_assignments a ON a.assignment_id = sr.assignment_id
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE vp.user_id = ?
                ORDER BY sr.service_date DESC
                """;
        List<ServiceHour> hours = new ArrayList<>();
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    hours.add(readServiceHour(result));
                }
            }
        }
        return hours;
    }

    private List<Skill> readSkills(Connection connection, String sql, int volunteerId)
            throws SQLException {
        List<Skill> skills = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, volunteerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    skills.add(new Skill(
                            result.getInt("skill_id"),
                            result.getString("skill_name"),
                            result.getString("skill_level")));
                }
            }
        }
        return skills;
    }

    private List<AvailabilitySlot> readAvailability(
            Connection connection, String sql, int volunteerId) throws SQLException {
        List<AvailabilitySlot> slots = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, volunteerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    slots.add(new AvailabilitySlot(
                            DayOfWeek.valueOf(result.getString("day_of_week")),
                            result.getObject("start_time", java.time.LocalTime.class),
                            result.getObject("end_time", java.time.LocalTime.class)));
                }
            }
        }
        return slots;
    }

    private int findVolunteerId(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT volunteer_id FROM volunteer_profiles WHERE user_id = ?")) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Volunteer profile was not found.");
                }
                return result.getInt(1);
            }
        }
    }

    private List<Skill> getTaskSkills(Connection connection, int taskId) throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = """
                SELECT s.skill_id, s.skill_name
                FROM task_required_skills trs
                JOIN skills s ON s.skill_id = trs.skill_id
                WHERE trs.task_id = ?
                ORDER BY s.skill_name
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    skills.add(new Skill(result.getInt("skill_id"),
                            result.getString("skill_name"), Skill.ANY_LEVEL));
                }
            }
        }
        return skills;
    }

    private VolunteerAssignment readAssignment(ResultSet result) throws SQLException {
        return new VolunteerAssignment(
                result.getInt("assignment_id"),
                result.getInt("task_id"),
                result.getInt("user_id"),
                result.getString("name"),
                result.getString("description"),
                result.getTimestamp("assigned_at").toLocalDateTime(),
                result.getTimestamp("starts_at").toLocalDateTime(),
                result.getTimestamp("ends_at").toLocalDateTime(),
                result.getString("status"));
    }

    private void loadServiceHours(
            Connection connection, List<VolunteerAssignment> assignments) throws SQLException {
        if (assignments.isEmpty()) {
            return;
        }

        Map<Integer, VolunteerAssignment> assignmentsById = new LinkedHashMap<>();
        for (VolunteerAssignment assignment : assignments) {
            assignmentsById.put(assignment.id(), assignment);
        }
        String placeholders = String.join(
                ",", java.util.Collections.nCopies(assignments.size(), "?"));
        String sql = """
                SELECT sr.service_record_id, sr.assignment_id, u.name AS volunteer_name,
                       sr.service_date, sr.hours, t.description, sr.status
                FROM service_records sr
                JOIN volunteer_assignments a ON a.assignment_id = sr.assignment_id
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE sr.assignment_id IN (%s)
                ORDER BY sr.service_date, sr.service_record_id
                """.formatted(placeholders);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (VolunteerAssignment assignment : assignments) {
                statement.setInt(parameter++, assignment.id());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ServiceHour serviceHour = readServiceHour(result);
                    VolunteerAssignment assignment =
                            assignmentsById.get(serviceHour.assignmentId());
                    if (assignment != null) {
                        assignment.addServiceHour(serviceHour);
                    }
                }
            }
        }
    }

    private VolunteerAssignment getAssignment(Connection connection, int taskId, int volunteerId)
            throws SQLException {
        String sql = """
                SELECT a.assignment_id, a.task_id, vp.user_id, u.name,
                       a.assigned_at, t.description, t.starts_at, t.ends_at, a.status
                FROM volunteer_assignments a
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN users u ON u.user_id = vp.user_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE a.task_id = ? AND a.volunteer_id = ? AND a.status = 'ASSIGNED'
                """;
        VolunteerAssignment assignment;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            statement.setInt(2, volunteerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The volunteer assignment could not be loaded.");
                }
                assignment = readAssignment(result);
            }
        }
        loadServiceHours(connection, List.of(assignment));
        return assignment;
    }

    private ServiceHour readServiceHour(ResultSet result) throws SQLException {
        return new ServiceHour(
                result.getInt("service_record_id"),
                result.getInt("assignment_id"),
                result.getString("volunteer_name"),
                result.getDate("service_date").toLocalDate(),
                result.getBigDecimal("hours"),
                result.getString("description"),
                result.getString("status"));
    }

    private boolean isAlreadyAssigned(Connection connection, int taskId, int volunteerId)
            throws SQLException {
        String sql = """
                SELECT status = 'ASSIGNED'
                FROM volunteer_assignments
                WHERE task_id = ? AND volunteer_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, taskId);
            statement.setInt(2, volunteerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void rollback(Connection connection, Exception exception) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            exception.addSuppressed(rollbackException);
        }
    }

}

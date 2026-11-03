package org.catalyst.repo;

import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerAssignment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

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

        volunteer.updateSkills(readStrings(connection, """
                SELECT s.skill_name
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

    public List<String> getSkills() throws SQLException {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM skills ORDER BY skill_name";
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                skills.add(result.getString("skill_name"));
            }
        }
        return skills;
    }

    public String addSkill(String name) throws SQLException {
        String sql = """
                INSERT INTO skills (skill_name) VALUES (?)
                ON CONFLICT (skill_name) DO UPDATE SET skill_name = EXCLUDED.skill_name
                RETURNING skill_name
                """;
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The skill could not be saved.");
                }
                return result.getString("skill_name");
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
            for (String skill : volunteer.getSkills()) {
                int skillId = findOrCreateSkill(connection, skill);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO volunteer_skills (volunteer_id, skill_id) VALUES (?, ?)")) {
                    statement.setInt(1, volunteerId);
                    statement.setInt(2, skillId);
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
                    statement.setTime(3, Time.valueOf(slot.start()));
                    statement.setTime(4, Time.valueOf(slot.end()));
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
                SELECT a.assignment_id, a.status, t.description, t.starts_at, t.ends_at
                FROM volunteer_assignments a
                JOIN volunteer_profiles vp ON vp.volunteer_id = a.volunteer_id
                JOIN volunteer_tasks t ON t.task_id = a.task_id
                WHERE vp.user_id = ?
                ORDER BY t.starts_at
                """;
        List<VolunteerAssignment> assignments = new ArrayList<>();
        try (PreparedStatement statement =
                     DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    assignments.add(new VolunteerAssignment(
                            result.getInt("assignment_id"),
                            result.getString("description"),
                            result.getTimestamp("starts_at").toLocalDateTime(),
                            result.getTimestamp("ends_at").toLocalDateTime(),
                            result.getString("status")));
                }
            }
        }
        return assignments;
    }

    public List<ServiceHour> getServiceHours(int userId) throws SQLException {
        String sql = """
                SELECT sr.service_date, sr.hours, t.description
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
                    hours.add(new ServiceHour(
                            result.getDate("service_date").toLocalDate(),
                            result.getBigDecimal("hours"),
                            result.getString("description")));
                }
            }
        }
        return hours;
    }

    private List<String> readStrings(Connection connection, String sql, int volunteerId)
            throws SQLException {
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, volunteerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(result.getString(1));
                }
            }
        }
        return values;
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
                            result.getTime("start_time").toLocalTime(),
                            result.getTime("end_time").toLocalTime()));
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

    private int findOrCreateSkill(Connection connection, String name) throws SQLException {
        String sql = """
                INSERT INTO skills (skill_name) VALUES (?)
                ON CONFLICT (skill_name) DO UPDATE SET skill_name = EXCLUDED.skill_name
                RETURNING skill_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}

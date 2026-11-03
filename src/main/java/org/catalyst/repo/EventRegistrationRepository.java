package org.catalyst.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class EventRegistrationRepository {

    public int insertConfirmed(
            Connection connection, int eventId, int userId, String ticketReference)
            throws SQLException {
        String sql = """
                INSERT INTO event_registrations
                    (event_id, user_id, registration_status, ticket_reference)
                VALUES (?, ?, 'CONFIRMED', ?)
                RETURNING registration_id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, userId);
            statement.setString(3, ticketReference);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The database did not return a registration ID.");
                }
                return result.getInt("registration_id");
            }
        }
    }
    public boolean cancel(Connection connection, int registrationId, int userId)
            throws SQLException {
        String sql = """
            UPDATE event_registrations
            SET registration_status = 'CANCELLED'
            WHERE registration_id = ?
              AND user_id = ?
              AND registration_status IN ('PENDING', 'CONFIRMED')
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, registrationId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }
    public int insertPending(Connection connection, int eventId, int userId)
            throws SQLException {
        String sql = """
            INSERT INTO event_registrations
                (event_id, user_id, registration_status)
            VALUES (?, ?, 'PENDING')
            RETURNING registration_id
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            statement.setInt(2, userId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The database did not return a registration ID.");
                }
                return result.getInt("registration_id");
            }
        }
    }
}
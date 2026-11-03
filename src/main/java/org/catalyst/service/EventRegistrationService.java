package org.catalyst.service;

import org.catalyst.repo.DatabaseConnection;
import org.catalyst.repo.EventRegistrationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.math.BigDecimal;

public final class EventRegistrationService {
    private final EventRegistrationRepository registrationRepository;
    private record RegistrationTerms(int capacity, BigDecimal ticketPrice) {}

    public EventRegistrationService(EventRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public int register(int userId, int eventId) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            RegistrationTerms terms = lockUpcomingEvent(connection, eventId);

            if (countReservedSeats(connection, eventId) >= terms.capacity()) {
                throw new IllegalStateException("This event is full.");
            }

            int registrationId;
            if (terms.ticketPrice().signum() == 0) {
                String ticketReference = UUID.randomUUID().toString();
                registrationId = registrationRepository.insertConfirmed(
                        connection, eventId, userId, ticketReference);
            } else {
                registrationId = registrationRepository.insertPending(
                        connection, eventId, userId);
            }

            connection.commit();
            return registrationId;
        } catch (SQLException | RuntimeException exception) {
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

    private RegistrationTerms lockUpcomingEvent(Connection connection, int eventId)
            throws SQLException {
        String sql = """
                SELECT status, capacity, ticket_price
                FROM events
                WHERE event_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Event not found: " + eventId);
                }
                if (!"UPCOMING".equals(result.getString("status"))) {
                    throw new IllegalStateException("This event is closed for registration.");
                }
                if (result.getBigDecimal("ticket_price").signum() != 0) {
                    throw new IllegalStateException("Paid registration is not available yet.");
                }
                return new RegistrationTerms(
                        result.getInt("capacity"),
                        result.getBigDecimal("ticket_price")
                );
            }
        }
    }

    private int countReservedSeats(Connection connection, int eventId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM event_registrations
                WHERE event_id = ?
                  AND registration_status <> 'CANCELLED'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
    public void cancelRegistration(int userId, int registrationId)
            throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();

        if (!registrationRepository.cancel(connection, registrationId, userId)) {
            throw new IllegalStateException(
                    "No active registration was found for this user.");
        }
    }
}
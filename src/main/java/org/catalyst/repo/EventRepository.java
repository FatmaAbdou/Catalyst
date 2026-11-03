package org.catalyst.repo;


import org.catalyst.model.Event;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import org.catalyst.model.EventDetails;
import org.catalyst.model.EventSummary;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EventRepository {

    public int save(Event event) throws SQLException {
        String sql = """
                INSERT INTO events
                    (name, description, location, starts_at, ends_at, capacity, ticket_price)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING event_id
                """;

        LocalDate date = new Date(event.getDate().getTime()).toLocalDate();
        Connection connection = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.getName());
            statement.setString(2, event.getDescription());
            statement.setString(3, event.getLocation());
            statement.setObject(4, date.atTime(event.getStartTime().toLocalTime()));
            statement.setObject(5, date.atTime(event.getEndTime().toLocalTime()));
            statement.setInt(6, event.getCapacity());
            statement.setBigDecimal(7, new BigDecimal(Float.toString(event.getTicketPrice())));

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The database did not return the new event ID.");
                }
                return result.getInt("event_id");
            }
        }
    }

    public boolean update(int eventId, EventDetails details) throws SQLException {
        String sql = """
            UPDATE events
            SET name = ?, description = ?, location = ?,
                starts_at = ?, ends_at = ?, capacity = ?, ticket_price = ?
            WHERE event_id = ?
            """;

        LocalDate date = new Date(details.date().getTime()).toLocalDate();
        Connection connection = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, details.name());
            statement.setString(2, details.description());
            statement.setString(3, details.location());
            statement.setObject(4, date.atTime(details.startTime().toLocalTime()));
            statement.setObject(5, date.atTime(details.endTime().toLocalTime()));
            statement.setInt(6, details.capacity());
            statement.setBigDecimal(7, new BigDecimal(Float.toString(details.ticketPrice())));
            statement.setInt(8, eventId);

            return statement.executeUpdate() == 1;
        }
    }

    public List<EventSummary> findUpcoming() throws SQLException {
        String sql = """
            SELECT event_id, name, location, starts_at, ticket_price
            FROM events
            WHERE status = 'UPCOMING'
            ORDER BY starts_at, event_id
            """;

        Connection connection = DatabaseConnection.getInstance().getConnection();
        List<EventSummary> events = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                events.add(new EventSummary(
                        result.getInt("event_id"),
                        result.getString("name"),
                        result.getString("location"),
                        result.getObject("starts_at", LocalDateTime.class),
                        result.getBigDecimal("ticket_price")
                ));
            }
        }

        return events;
    }
}
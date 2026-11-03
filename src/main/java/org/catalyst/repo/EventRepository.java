package org.catalyst.repo;


import org.catalyst.model.Event;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

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
}
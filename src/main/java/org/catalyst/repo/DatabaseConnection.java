package org.catalyst.repo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Provides one shared PostgreSQL connection for the application. */
public final class DatabaseConnection {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private Connection connection;

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    /** Returns the shared connection, opening it the first time it is needed. */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String host = requiredEnv("DB_HOST");
            String database = requiredEnv("DB_NAME");
            String username = requiredEnv("DB_USER");
            String password = requiredEnv("DB_PASSWORD");
            String url = "jdbc:postgresql://" + host + "/" + database + "?sslmode=require";
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    /** Closes the shared connection when the application exits. */
    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        connection = null;
    }

    private String requiredEnv(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing required environment variable: " + name);
        }
        return value;
    }
}

package org.catalyst;

import org.flywaydb.core.Flyway;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Runs the versioned SQL migrations against the configured PostgreSQL database. */
public final class DatabaseMigrationRunner {
    private DatabaseMigrationRunner() {
    }

    public static void main(String[] args) {
        String host = requiredEnv("DB_HOST");
        String database = requiredEnv("DB_NAME");
        String username = requiredEnv("DB_USER");
        String password = requiredEnv("DB_PASSWORD");

        String jdbcUrl = "jdbc:postgresql://" + host + "/" + database + "?sslmode=require";

        try (var connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (var statement = connection.prepareStatement("""
            SELECT version, description, script, checksum, success
            FROM flyway_schema_history
            WHERE version = '6'
            """);
                 var result = statement.executeQuery()) {
                while (result.next()) {
                    System.out.printf("V%s: %s, checksum=%d, success=%s%n",
                            result.getString("version"),
                            result.getString("script"),
                            result.getInt("checksum"),
                            result.getBoolean("success"));
                }
            }

            try (var statement = connection.prepareStatement("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = 'volunteer_skills'
              AND column_name = 'skill_level'
            """);
                 var result = statement.executeQuery()) {
                System.out.println("skill_level column exists: " + result.next());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();
        System.out.println("Flyway migration completed.");
        System.out.println("Migrations applied: " + result.migrationsExecuted);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}

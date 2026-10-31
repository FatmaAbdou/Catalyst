package org.catalyst.db;

import org.flywaydb.core.Flyway;

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

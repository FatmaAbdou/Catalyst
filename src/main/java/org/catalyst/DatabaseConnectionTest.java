
package org.catalyst;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnectionTest {

    public static void main(String[] args) {
        String host = System.getenv("DB_HOST");
        String database = System.getenv("DB_NAME");
        String username = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (host == null || database == null
                || username == null || password == null) {
            System.err.println(
                    "Database configuration is incomplete. "
                            + "Check the environment variables in IntelliJ."
            );
            return;
        }

        String url = "jdbc:postgresql://" + host + "/" + database
                + "?sslmode=require&channel_binding=require";

        try (Connection connection =
                     DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT current_database(), current_user, version()")) {

            if (result.next()) {
                System.out.println("Connected to Neon successfully!");
                System.out.println("Database: " + result.getString(1));
                System.out.println("User: " + result.getString(2));
                System.out.println("PostgreSQL: " + result.getString(3));
            }

        } catch (Exception e) {
            System.err.println("Database connection failed:");
            System.err.println(e.getMessage());
        }
    }
}


package org.catalyst;

import java.sql.ResultSet;
import java.sql.Statement;
import org.catalyst.repo.DatabaseConnection;

/** Checks database connectivity using the application's shared connection. */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        try {
            var connection = DatabaseConnection.getInstance().getConnection();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT current_database(), current_user, version()")) {

                if (result.next()) {
                    System.out.println("Connected to Neon successfully!");
                    System.out.println("Database: " + result.getString(1));
                    System.out.println("User: " + result.getString(2));
                    System.out.println("PostgreSQL: " + result.getString(3));
                }
            }
        } catch (Exception e) {
            System.err.println("Database connection failed:");
            System.err.println(e.getMessage());
        }
    }
}

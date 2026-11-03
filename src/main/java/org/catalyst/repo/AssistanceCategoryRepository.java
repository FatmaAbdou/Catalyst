package org.catalyst.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only category choices for recording delivered assistance. */
public final class AssistanceCategoryRepository {
    public List<String> findAllNames() throws SQLException {
        String sql = "SELECT category_name FROM assistance_categories ORDER BY lower(category_name), category_id";
        Connection connection = DatabaseConnection.getInstance().getConnection();
        List<String> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                categories.add(result.getString("category_name"));
            }
        }
        return List.copyOf(categories);
    }
}

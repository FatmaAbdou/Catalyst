package org.catalyst.repo;

import org.catalyst.model.Donor;
import org.catalyst.model.User;
import org.catalyst.model.UserFactory;
import org.catalyst.model.UserType;
import org.catalyst.model.Volunteer;
import org.catalyst.service.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/** Database operations for creating accounts and checking login credentials. */
public final class UserRepository {

    public void create(User user, String passwordHash) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int userId = insertUser(connection, user, passwordHash);
            assignRole(connection, userId, user);
            insertProfile(connection, userId, user);
            connection.commit();
            user.setId(userId);
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            if ("23505".equals(exception.getSQLState())) {
                throw new IllegalArgumentException(
                        "An account with this email already exists.", exception);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    public Optional<User> authenticate(
            String email, char[] password, PasswordHasher passwordHasher, String dummyPasswordHash)
            throws SQLException {
        String sql = """
                SELECT u.user_id, u.name, u.email, u.contact_info,
                       u.password_hash, u.status, d.organization
                FROM users u
                LEFT JOIN donors d ON d.user_id = u.user_id
                WHERE LOWER(u.email) = ?
                """;
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.toLowerCase(Locale.ROOT));
            int userId;
            String name;
            String storedEmail;
            String contactInfo;
            String passwordHash;
            boolean active;
            String organization;
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    passwordHasher.verify(password, dummyPasswordHash);
                    return Optional.empty();
                }
                userId = result.getInt("user_id");
                name = result.getString("name");
                storedEmail = result.getString("email");
                contactInfo = result.getString("contact_info");
                passwordHash = result.getString("password_hash");
                active = "ACTIVE".equals(result.getString("status"));
                organization = result.getString("organization");
            }
            if (!passwordHasher.verify(password, passwordHash) || !active) {
                return Optional.empty();
            }
            Optional<UserType> userType = findUserType(connection, userId);
            if (userType.isEmpty()) {
                return Optional.empty();
            }
            User user = UserFactory.createUser(
                    userType.get(), name, storedEmail, contactInfo, organization);
            user.setId(userId);
            return Optional.of(user);
        }
    }

    private int insertUser(Connection connection, User user, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO users (name, email, password_hash, contact_info)
                VALUES (?, ?, ?, ?)
                RETURNING user_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, passwordHash);
            statement.setString(4, user.getContactInfo());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The database did not return the new user ID.");
                }
                return result.getInt(1);
            }
        }
    }

    private void assignRole(Connection connection, int userId, User user) throws SQLException {
        String sql = """
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, role_id FROM roles WHERE role_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, user.getUserType().name());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The selected account role has not been added to the database.");
            }
        }
    }

    private void insertProfile(Connection connection, int userId, User user) throws SQLException {
        if (user instanceof Donor donor) {
            String donorType = donor.getOrganization().isBlank() ? "INDIVIDUAL" : "ORGANIZATION";
            String sql = """
                    INSERT INTO donors (user_id, donor_name, donor_type, contact_info, organization)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setString(2, donor.getName());
                statement.setString(3, donorType);
                statement.setString(4, donor.getContactInfo());
                statement.setString(5, donor.getOrganization().isBlank()
                        ? null : donor.getOrganization());
                statement.executeUpdate();
            }
        } else if (user instanceof Volunteer) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO volunteer_profiles (user_id) VALUES (?)")) {
                statement.setInt(1, userId);
                statement.executeUpdate();
            }
        }
    }

    private Optional<UserType> findUserType(Connection connection, int userId) throws SQLException {
        String sql = """
                SELECT r.role_name
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.role_id
                WHERE ur.user_id = ? AND r.role_name IN ('DONOR', 'VOLUNTEER')
                ORDER BY r.role_name
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(UserType.valueOf(result.getString("role_name")));
                } catch (IllegalArgumentException exception) {
                    return Optional.empty();
                }
            }
        }
    }
}

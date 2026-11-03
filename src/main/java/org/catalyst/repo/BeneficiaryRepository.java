package org.catalyst.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.catalyst.model.beneficiary.Beneficiary;

public final class BeneficiaryRepository {

    public void create(Beneficiary beneficiary) throws SQLException {
        if (beneficiary == null
                || beneficiary.getBeneficiaryID() != 0) {

            throw new IllegalArgumentException(
                    "Provide a new beneficiary without an assigned ID."
            );
        }

        String sql = """
                INSERT INTO beneficiaries (
                    beneficiary_reference,
                    name,
                    contact_info,
                    household_size,
                    needs
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING beneficiary_id
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        int generatedID;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, "BEN-" + UUID.randomUUID());
            statement.setString(2, beneficiary.getName());
            statement.setString(3, beneficiary.getContactInfo());
            statement.setInt(4, beneficiary.getHouseholdSize());
            statement.setString(5, beneficiary.getNeeds());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(
                            "The database did not return a beneficiary ID."
                    );
                }

                generatedID = result.getInt("beneficiary_id");
            }
        }

        beneficiary.setBeneficiaryID(generatedID);
    }

    public void update(Beneficiary beneficiary) throws SQLException {
        if (beneficiary == null
                || beneficiary.getBeneficiaryID() <= 0) {

            throw new IllegalArgumentException(
                    "Provide a beneficiary with an assigned ID."
            );
        }

        String sql = """
                UPDATE beneficiaries
                SET name = ?,
                    contact_info = ?,
                    household_size = ?,
                    needs = ?
                WHERE beneficiary_id = ?
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, beneficiary.getName());
            statement.setString(2, beneficiary.getContactInfo());
            statement.setInt(3, beneficiary.getHouseholdSize());
            statement.setString(4, beneficiary.getNeeds());
            statement.setInt(5, beneficiary.getBeneficiaryID());

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "The beneficiary does not exist."
                );
            }
        }
    }

    public Optional<Beneficiary> findById(int beneficiaryID)
            throws SQLException {

        String sql = """
                SELECT beneficiary_id, name, contact_info,
                       household_size, needs
                FROM beneficiaries
                WHERE beneficiary_id = ?
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, beneficiaryID);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapBeneficiary(result));
            }
        }
    }

    public List<Beneficiary> search(String keyword)
            throws SQLException {

        String search = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        String sql = """
                SELECT beneficiary_id, name, contact_info,
                       household_size, needs
                FROM beneficiaries
                WHERE strpos(lower(name), ?) > 0
                   OR strpos(lower(coalesce(contact_info, '')), ?) > 0
                   OR strpos(lower(coalesce(needs, '')), ?) > 0
                ORDER BY lower(name), beneficiary_id
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        List<Beneficiary> beneficiaries = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    beneficiaries.add(mapBeneficiary(result));
                }
            }
        }

        return List.copyOf(beneficiaries);
    }

    /** Converts the current database result row into a Beneficiary object. */
    private Beneficiary mapBeneficiary(ResultSet result)
            throws SQLException {

        Beneficiary beneficiary = new Beneficiary(
                result.getString("name"),
                result.getString("contact_info"),
                result.getInt("household_size"),
                result.getString("needs")
        );

        beneficiary.setBeneficiaryID(
                result.getInt("beneficiary_id")
        );

        return beneficiary;
    }
}

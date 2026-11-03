package org.catalyst.repo;

import org.catalyst.model.AssistanceFactory;
import org.catalyst.model.AssistanceRecord;
import org.catalyst.model.InKindAssistance;
import org.catalyst.model.MonetaryAssistance;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AssistanceRepository {

    private static final String SELECT_RECORDS = """
            SELECT a.assistance_id,
                   a.assistance_date,
                   a.assistance_type,
                   a.amount,
                   a.quantity,
                   a.unit,
                   a.description,
                   a.notes,
                   c.category_name
            FROM assistance_distributions a
            JOIN assistance_categories c
                ON c.category_id = a.category_id
            """;

    private final AssistanceFactory factory =
            new AssistanceFactory();

    public void create(
            int beneficiaryID,
            AssistanceRecord record) throws SQLException {

        create(beneficiaryID, record, null);
    }

    public void create(
            int beneficiaryID,
            AssistanceRecord record,
            Integer campaignID) throws SQLException {

        requirePositiveID(beneficiaryID);

        if (campaignID != null) {
            requirePositiveID(campaignID);
        }

        String type = validateNewRecord(record);

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        int categoryID =
                findCategoryID(connection, record.getCategory());

        String sql = """
                INSERT INTO assistance_distributions (
                    beneficiary_id,
                    category_id,
                    campaign_id,
                    assistance_date,
                    assistance_type,
                    amount,
                    quantity,
                    unit,
                    description,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING assistance_id
                """;

        int generatedID;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, beneficiaryID);
            statement.setInt(2, categoryID);

            if (campaignID == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, campaignID);
            }

            statement.setObject(4, record.getDate());
            statement.setString(5, type);

            if (record instanceof MonetaryAssistance monetary) {
                statement.setBigDecimal(6, monetary.getAmount());

                statement.setNull(7, Types.NUMERIC);
                statement.setNull(8, Types.VARCHAR);
                statement.setNull(9, Types.VARCHAR);
            } else {
                InKindAssistance inKind =
                        (InKindAssistance) record;

                statement.setNull(6, Types.NUMERIC);
                statement.setBigDecimal(
                        7,
                        BigDecimal.valueOf(inKind.getQuantity())
                );
                statement.setString(8, inKind.getUnit());
                statement.setString(9, inKind.getItemDescription());
            }

            statement.setString(10, record.getNotes());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(
                            "The database did not return an assistance ID."
                    );
                }

                generatedID = result.getInt("assistance_id");
            }
        }

        record.setRecordID(generatedID);
    }

    public Optional<AssistanceRecord> findById(int recordID)
            throws SQLException {

        requirePositiveID(recordID);

        String sql = SELECT_RECORDS
                + " WHERE a.assistance_id = ?";

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, recordID);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapAssistance(result));
            }
        }
    }

    public List<AssistanceRecord> findByBeneficiaryId(
            int beneficiaryID) throws SQLException {

        requirePositiveID(beneficiaryID);

        String sql = SELECT_RECORDS + """
                 WHERE a.beneficiary_id = ?
                 ORDER BY a.assistance_date DESC, a.assistance_id DESC
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        List<AssistanceRecord> history = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, beneficiaryID);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    history.add(mapAssistance(result));
                }
            }
        }

        return List.copyOf(history);
    }

    public String appendNote(int recordID, String notes)
            throws SQLException {

        requirePositiveID(recordID);

        String cleanNotes = notes == null ? "" : notes.trim();

        if (cleanNotes.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter a follow-up note."
            );
        }

        String sql = """
                UPDATE assistance_distributions
                SET notes = concat_ws(chr(10), nullif(notes, ''), ?)
                WHERE assistance_id = ?
                RETURNING notes
                """;

        Connection connection =
                DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, cleanNotes);
            statement.setInt(2, recordID);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "The assistance record does not exist."
                    );
                }

                return result.getString("notes");
            }
        }
    }

    private int findCategoryID(
            Connection connection,
            String category) throws SQLException {

        String sql = """
                SELECT category_id
                FROM assistance_categories
                WHERE category_name = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, category);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Choose an existing assistance category: "
                                    + category
                    );
                }

                return result.getInt("category_id");
            }
        }
    }

    private AssistanceRecord mapAssistance(ResultSet result)
            throws SQLException {

        try {
            AssistanceRecord record = factory.createAssistance(
                    result.getString("assistance_type")
            );

            record.setRecordID(result.getInt("assistance_id"));

            var date = result.getDate("assistance_date");

            if (date == null) {
                throw new SQLException(
                        "The saved assistance record has no delivery date."
                );
            }

            record.setDate(date.toLocalDate());
            record.setCategory(result.getString("category_name"));
            record.setNotes(result.getString("notes"));

            if (record instanceof MonetaryAssistance monetary) {
                monetary.setAmount(result.getBigDecimal("amount"));
            } else {
                BigDecimal quantity =
                        result.getBigDecimal("quantity");

                if (quantity == null) {
                    throw new SQLException(
                            "The saved in-kind record has no quantity."
                    );
                }

                ((InKindAssistance) record).setItemDetails(
                        result.getString("description"),
                        quantity.intValueExact(),
                        result.getString("unit")
                );
            }

            return record;

        } catch (IllegalArgumentException
                 | IllegalStateException
                 | ArithmeticException exception) {

            throw new SQLException(
                    "The saved assistance record cannot be mapped "
                            + "to the current model.",
                    exception
            );
        }
    }

    private String validateNewRecord(AssistanceRecord record) {
        if (record == null || record.getRecordID() != 0) {
            throw new IllegalArgumentException(
                    "Provide a new record without an assigned ID."
            );
        }

        if (record.getDate() == null
                || record.getCategory() == null
                || record.getCategory().isBlank()) {

            throw new IllegalArgumentException(
                    "Set the delivery date and category."
            );
        }

        if (record instanceof MonetaryAssistance monetary) {
            if (monetary.getAmount() == null) {
                throw new IllegalArgumentException(
                        "Set the monetary assistance amount."
                );
            }

            return "MONETARY";
        }

        if (record instanceof InKindAssistance inKind) {
            if (inKind.getItemDescription() == null
                    || inKind.getUnit() == null
                    || inKind.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Set the in-kind assistance details."
                );
            }

            if (inKind.getQuantity() > 999_999_999) {
                throw new IllegalArgumentException(
                        "Quantity exceeds the database's capacity."
                );
            }

            return "IN_KIND";
        }

        throw new IllegalArgumentException(
                "Unsupported assistance record type."
        );
    }

    private void requirePositiveID(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "The ID must be positive."
            );
        }
    }
}

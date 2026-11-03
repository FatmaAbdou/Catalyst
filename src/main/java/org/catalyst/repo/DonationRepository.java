package org.catalyst.repo;

import org.catalyst.model.donation.Donation;
import org.catalyst.model.donation.InKindDonation;
import org.catalyst.model.donation.MonetaryDonation;
import org.catalyst.model.user.Donor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/** Database operations for donations and their payments. */
public final class DonationRepository {

    /** Finds an active donor account by email so an officer can record a donation for them. */
    public Optional<Donor> findDonorByEmail(String email) throws SQLException {
        String sql = """
                SELECT u.user_id, u.name, u.email, u.contact_info, d.organization
                FROM users u
                JOIN donors d ON d.user_id = u.user_id
                WHERE LOWER(u.email) = ? AND u.status = 'ACTIVE'
                """;
        Connection connection = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String organization = result.getString("organization");
                Donor donor = new Donor(
                        result.getString("name"),
                        result.getString("email"),
                        result.getString("contact_info"),
                        organization == null ? "" : organization);
                donor.setId(result.getInt("user_id"));
                return Optional.of(donor);
            }
        }
    }

    /**
     * Saves a processed donation. The donation row, its type-specific row and (for money) the
     * payment transaction are written in one transaction, so either all of them exist or none do.
     */
    public void save(Donation donation) throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int donationId = insertDonation(connection, donation);
            if (donation instanceof MonetaryDonation monetary) {
                insertMonetaryDonation(connection, donationId, monetary);
                insertPaymentTransaction(connection, donationId, monetary);
            } else if (donation instanceof InKindDonation inKind) {
                insertInKindDonation(connection, donationId, inKind);
            } else {
                throw new IllegalArgumentException("Unsupported donation type.");
            }
            connection.commit();
            donation.setId(donationId);
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /** donations.donor_id points at the donors table, so it is looked up from the user's id. */
    private int insertDonation(Connection connection, Donation donation) throws SQLException {
        String sql = """
                INSERT INTO donations (donor_id, donation_reference, donation_type, donation_date)
                SELECT donor_id, ?, ?, ? FROM donors WHERE user_id = ?
                RETURNING donation_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, donation.getReference());
            statement.setString(2, donation.getType().name());
            statement.setObject(3, donation.getDate());
            statement.setInt(4, donation.getDonor().getId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("This account has no donor profile.");
                }
                return result.getInt(1);
            }
        }
    }

    private void insertMonetaryDonation(
            Connection connection, int donationId, MonetaryDonation donation) throws SQLException {
        String sql = """
                INSERT INTO monetary_donations (donation_id, amount, payment_method)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            statement.setBigDecimal(2, donation.getAmount());
            statement.setString(3, donation.getPaymentMethod().getDonationCategory());
            statement.executeUpdate();
        }
    }

    private void insertPaymentTransaction(
            Connection connection, int donationId, MonetaryDonation donation) throws SQLException {
        String sql = """
                INSERT INTO payment_transactions
                    (donation_id, provider_reference, payment_method, amount, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            statement.setString(2, donation.getPaymentResult().providerReference());
            statement.setString(3, donation.getPaymentMethod().name());
            statement.setBigDecimal(4, donation.getAmount());
            statement.setString(5, donation.getPaymentResult().status().name());
            statement.executeUpdate();
        }
    }

    private void insertInKindDonation(
            Connection connection, int donationId, InKindDonation donation) throws SQLException {
        String sql = """
                INSERT INTO in_kind_donations (donation_id, item_description, quantity, unit, condition)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            statement.setString(2, donation.getItemDescription());
            statement.setBigDecimal(3, donation.getQuantity());
            statement.setString(4, donation.getUnit());
            statement.setString(5, donation.getCondition());
            statement.executeUpdate();
        }
    }
}

package org.catalyst.service;

import org.catalyst.model.donation.Donation;
import org.catalyst.model.donation.DonationType;
import org.catalyst.model.donation.InKindDonation;
import org.catalyst.model.donation.MonetaryDonation;
import org.catalyst.model.payment.PaymentMethod;
import org.catalyst.model.payment.PaymentStrategy;
import org.catalyst.model.payment.PaymentStrategyFactory;
import org.catalyst.model.receipt.Branding;
import org.catalyst.model.receipt.QRCode;
import org.catalyst.model.receipt.Receipt;
import org.catalyst.model.receipt.TaxStatement;
import org.catalyst.model.receipt.ThankYouMessage;
import org.catalyst.model.user.DonationOfficer;
import org.catalyst.model.user.Donor;
import org.catalyst.model.user.User;
import org.catalyst.repo.DonationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decides who may donate, builds and processes the donation, saves it, and returns the
 * finished receipt. Donors give for themselves; donation officers record gifts for a donor.
 */
public final class DonationService {
    private static final Logger LOGGER = Logger.getLogger(DonationService.class.getName());

    private final DonationRepository donationRepository;

    public DonationService(DonationRepository donationRepository) {
        this.donationRepository = donationRepository;
    }

    public boolean canDonate(User actor) {
        return actor instanceof Donor || actor instanceof DonationOfficer;
    }

    /** True when the actor records donations for other people and must name the donor. */
    public boolean canRecordForOthers(User actor) {
        return actor instanceof DonationOfficer;
    }

    /** Donation types the actor may record. Donors give money online; only officers record goods. */
    public List<DonationType> availableTypes(User actor) {
        if (actor instanceof DonationOfficer) {
            return List.of(DonationType.values());
        }
        if (actor instanceof Donor) {
            return List.of(DonationType.MONETARY);
        }
        return List.of();
    }

    /** Payment methods the actor may use. Cash and checks are for officers only. */
    public List<PaymentMethod> availableMethods(User actor) {
        if (actor instanceof DonationOfficer) {
            return List.of(PaymentMethod.values());
        }
        if (actor instanceof Donor) {
            return Arrays.stream(PaymentMethod.values()).filter(method -> !method.isStaffOnly()).toList();
        }
        return List.of();
    }

    public Receipt donateMoney(
            User actor, String donorEmail, BigDecimal amount, PaymentMethod method, String paymentDetail)
            throws SQLException {
        Donor donor = resolveDonor(actor, donorEmail);
        if (method == null) {
            throw new IllegalArgumentException("Choose a payment method.");
        }
        if (!availableMethods(actor).contains(method)) {
            throw new IllegalArgumentException(
                    "Cash and check donations are recorded by a donation officer.");
        }
        PaymentStrategy strategy = PaymentStrategyFactory.create(method, paymentDetail);
        return complete(new MonetaryDonation(donor, amount, strategy));
    }

    public Receipt donateItems(
            User actor, String donorEmail, String itemDescription,
            BigDecimal quantity, String unit, String condition) throws SQLException {
        Donor donor = resolveDonor(actor, donorEmail);
        if (!availableTypes(actor).contains(DonationType.IN_KIND)) {
            throw new IllegalArgumentException("In-kind donations are recorded by a donation officer.");
        }
        return complete(new InKindDonation(donor, itemDescription, quantity, unit, condition));
    }

    private Donor resolveDonor(User actor, String donorEmail) throws SQLException {
        if (actor instanceof Donor donor) {
            return donor;
        }
        if (actor instanceof DonationOfficer) {
            if (donorEmail == null || donorEmail.isBlank()) {
                throw new IllegalArgumentException("Enter the donor's email address.");
            }
            return donationRepository.findDonorByEmail(donorEmail)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No active donor account uses that email address."));
        }
        throw new IllegalArgumentException("Only donors and donation officers can record donations.");
    }

    private Receipt complete(Donation donation) throws SQLException {
        Receipt receipt = donation.process();
        try {
            donationRepository.save(donation);
        } catch (SQLException | RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Donation " + donation.getReference()
                    + " was processed but could not be saved. Check it manually.", exception);
            throw exception;
        }
        return new Branding(new ThankYouMessage(new QRCode(new TaxStatement(receipt))));
    }
}
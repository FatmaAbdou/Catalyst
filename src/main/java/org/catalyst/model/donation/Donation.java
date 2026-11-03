package org.catalyst.model.donation;

import org.catalyst.model.receipt.BasicReceipt;
import org.catalyst.model.receipt.Receipt;
import org.catalyst.model.user.Donor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public abstract class Donation {
    
    public static final String CURRENCY = "EGP";

    private int id;
    private final String reference;
    private final Donor donor;
    private final LocalDate date;
    private boolean processed;

    protected Donation(Donor donor) {
        this.donor = Objects.requireNonNull(donor, "A donation needs a donor.");
        this.date = LocalDate.now();
        this.reference = "DON-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

/*
  Carries out the donation (for example, collects the payment) and returns its receipt.
  A donation can be processed only once, so it can never be charged twice.
 */
    public final Receipt process() {
        if (processed) {
            throw new IllegalStateException("This donation has already been processed.");
        }
        execute();
        processed = true;
        return new BasicReceipt(this);
    }

    protected abstract void execute();

    public abstract DonationType getType();

    /** Lines describing what was given, printed on the receipt. */
    public abstract List<String> describe();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public Donor getDonor() {
        return donor;
    }

    public LocalDate getDate() {
        return date;
    }
}

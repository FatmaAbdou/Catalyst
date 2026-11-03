package org.catalyst.model.donation;

import org.catalyst.model.payment.PaymentMethod;
import org.catalyst.model.payment.PaymentResult;
import org.catalyst.model.payment.PaymentStrategy;
import org.catalyst.model.user.Donor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A gift of money, collected through the chosen {@link PaymentStrategy}. */
public final class MonetaryDonation extends Donation {
    /** Largest value that fits {@code NUMERIC(12, 2)}. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final BigDecimal amount;
    private final PaymentStrategy paymentStrategy;
    private PaymentResult paymentResult;

    public MonetaryDonation(Donor donor, BigDecimal amount, PaymentStrategy paymentStrategy) {
        super(donor);
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Enter an amount greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amounts can have at most two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("That amount is too large to record.");
        }
        if (paymentStrategy == null) {
            throw new IllegalArgumentException("Choose a payment method.");
        }
        this.amount = amount;
        this.paymentStrategy = paymentStrategy;
    }

    @Override
    protected void execute() {
        PaymentResult result = paymentStrategy.pay(amount);
        if (!result.isConfirmed()) {
            throw new IllegalStateException(
                    "The payment was not confirmed, so no donation was recorded.");
        }
        paymentResult = result;
    }

    @Override
    public DonationType getType() {
        return DonationType.MONETARY;
    }

    @Override
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add("Amount: " + CURRENCY + " " + amount.toPlainString());
        lines.add("Payment method: " + paymentStrategy.getMethod());
        if (paymentResult != null && paymentResult.providerReference() != null) {
            lines.add("Payment reference: " + paymentResult.providerReference());
        }
        return lines;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentStrategy.getMethod();
    }

    public PaymentResult getPaymentResult() {
        return paymentResult;
    }
}

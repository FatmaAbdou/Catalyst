package org.catalyst.model.payment;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * A check received by a donation officer. The check number is kept as the payment reference.
 * The payment is recorded as confirmed when the officer enters it; whether the check later
 * clears is not tracked.
 */
public final class CheckPayment implements PaymentStrategy {
    private static final Pattern CHECK_NUMBER = Pattern.compile("[A-Za-z0-9-]{1,40}");

    private final String checkNumber;

    public CheckPayment(String checkNumber) {
        String clean = checkNumber == null ? "" : checkNumber.trim();
        if (!CHECK_NUMBER.matcher(clean).matches()) {
            throw new IllegalArgumentException(
                    "Enter the check number (letters, digits and dashes, up to 40 characters).");
        }
        this.checkNumber = clean;
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CHECK;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        return new PaymentResult(PaymentStatus.CONFIRMED, "CHECK-" + checkNumber);
    }
}

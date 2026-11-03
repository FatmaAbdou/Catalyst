package org.catalyst.model.payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class InstaPayPayment implements PaymentStrategy {

    private final String phoneNumber;

    public InstaPayPayment(String phoneNumber) {
        String clean = phoneNumber == null ? "" : phoneNumber.trim();
        if (!Pattern.matches("\\+?[0-9]{11,15}", clean)) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number (11 to 15 digits, optional leading +).");
        }
        this.phoneNumber = clean;
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.INSTAPAY;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        String reference = "INSTAPAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new PaymentResult(PaymentStatus.CONFIRMED, reference);
    }
}

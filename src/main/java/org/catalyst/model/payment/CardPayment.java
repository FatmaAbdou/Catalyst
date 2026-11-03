package org.catalyst.model.payment;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Locale;


public final class CardPayment implements PaymentStrategy {
    private final String lastFour;

    public CardPayment(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{12,19}")) {
            throw new IllegalArgumentException("Enter a valid card number.");
        }
        this.lastFour = digits.substring(digits.length() - 4);
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CARD;
    }

    public String getMaskedNumber() {
        return "**** " + lastFour;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        String reference = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new PaymentResult(PaymentStatus.CONFIRMED, reference);
    }
}

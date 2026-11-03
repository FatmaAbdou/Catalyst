package org.catalyst.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonetaryAssistance extends AssistanceRecord {

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "The assistance amount must be positive."
            );
        }

        BigDecimal normalizedAmount;

        try {
            normalizedAmount =
                    amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "The amount must have at most two decimal places.",
                    exception
            );
        }

        if (normalizedAmount.precision() > 12) {
            throw new IllegalArgumentException(
                    "The amount cannot exceed 9,999,999,999.99."
            );
        }

        this.amount = normalizedAmount;
    }

    @Override
    public String getSummary() {
        if (amount == null) {
            return "Monetary assistance: amount not set";
        }

        return "Monetary assistance: " + amount.toPlainString();
    }
}

package org.catalyst.model.payment;

import java.math.BigDecimal;

/** Cash handed to a donation officer. It is confirmed as soon as the officer records it. */
public final class CashPayment implements PaymentStrategy {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CASH;
    }

    @Override
    public PaymentResult pay(BigDecimal amount) {
        return new PaymentResult(PaymentStatus.CONFIRMED, null);
    }
}

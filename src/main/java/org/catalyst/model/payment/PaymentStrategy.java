package org.catalyst.model.payment;

import java.math.BigDecimal;

/** One way of collecting money for a donation (the Strategy in the payment design). */
public interface PaymentStrategy {

    PaymentMethod getMethod();

    PaymentResult pay(BigDecimal amount);
}

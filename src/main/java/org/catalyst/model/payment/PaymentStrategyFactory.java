package org.catalyst.model.payment;

/** Builds the payment strategy for the method chosen on the donation form. */
public final class PaymentStrategyFactory {
    private PaymentStrategyFactory() {
    }

    /**
     * @param method the chosen method
     * @param detail the method's extra detail: check number, card number or InstaPay address
     *               (ignored for cash)
     */
    public static PaymentStrategy create(PaymentMethod method, String detail) {
        if (method == null) {
            throw new IllegalArgumentException("Choose a payment method.");
        }
        return switch (method) {
            case CASH -> new CashPayment();
            case CHECK -> new CheckPayment(detail);
            case CARD -> new CardPayment(detail);
            case INSTAPAY -> new InstaPayPayment(detail);
        };
    }
}

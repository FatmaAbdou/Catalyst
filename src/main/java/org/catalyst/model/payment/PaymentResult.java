package org.catalyst.model.payment;

/**
 * Outcome of one payment attempt.
 *
 * @param status            whether the payment went through
 * @param providerReference gateway or check reference, or {@code null} when there is none (cash)
 */
public record PaymentResult(PaymentStatus status, String providerReference) {

    public boolean isConfirmed() {
        return status == PaymentStatus.CONFIRMED;
    }
    
}
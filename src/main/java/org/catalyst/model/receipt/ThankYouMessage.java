package org.catalyst.model.receipt;

/** Adds a closing thank-you line. */
public final class ThankYouMessage extends ReceiptDecorator {

    public ThankYouMessage(Receipt receipt) {
        super(receipt);
    }

    @Override
    public String PrintReceipt() {
        return super.PrintReceipt() + "\n\nThank you for your generosity. Your support makes a difference.";
    }
}

package org.catalyst.model.receipt;

import java.util.Objects;

/** Base for add-ons that wrap another receipt. By default it just passes the call through. */
public abstract class ReceiptDecorator implements Receipt {
    protected final Receipt receipt;

    protected ReceiptDecorator(Receipt receipt) {
        this.receipt = Objects.requireNonNull(receipt, "A decorator needs a receipt to wrap.");
    }

    @Override
    public String getReference() {
        return receipt.getReference();
    }

    @Override
    public String PrintReceipt() {
        return receipt.PrintReceipt();
    }
}

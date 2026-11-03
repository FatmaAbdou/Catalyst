package org.catalyst.model.receipt;

/** Adds the organization header above the receipt. */
public final class Branding extends ReceiptDecorator {
    private static final String HEADER = """
            ==============================
             CATALYST - COMMUNITY CARE
             Donation receipt
            ==============================""";

    public Branding(Receipt receipt) {
        super(receipt);
    }

    @Override
    public String PrintReceipt() {
        return HEADER + "\n" + super.PrintReceipt();
    }
}

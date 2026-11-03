package org.catalyst.model.receipt;

/** Adds a note asking the donor to keep the receipt for their tax records. */
public final class TaxStatement extends ReceiptDecorator {

    public TaxStatement(Receipt receipt) {
        super(receipt);
    }

    @Override
    public String PrintReceipt() {
        return super.PrintReceipt() + "\n\nTax statement: Please keep this receipt for your records. "
                + "Ask your tax adviser whether this donation qualifies for any tax relief.";
    }
}

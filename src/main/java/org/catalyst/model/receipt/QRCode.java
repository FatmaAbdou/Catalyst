package org.catalyst.model.receipt;

/**
 * Adds the verification code of the receipt. This prints the text a QR code would carry;
 * drawing the actual picture needs a QR library such as ZXing, which is not a dependency yet.
 */
public final class QRCode extends ReceiptDecorator {

    public QRCode(Receipt receipt) {
        super(receipt);
    }

    @Override
    public String PrintReceipt() {
        return super.PrintReceipt() + "\n\nVerification code (QR payload): catalyst://receipt/" + getReference();
    }
}

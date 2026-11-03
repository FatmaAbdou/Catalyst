package org.catalyst.model.receipt;

/** A printable donation receipt (the Component in the Decorator design). */
public interface Receipt {

    /** The donation reference this receipt belongs to. */
    String getReference();

    String PrintReceipt();
}

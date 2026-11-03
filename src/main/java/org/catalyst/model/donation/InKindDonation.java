package org.catalyst.model.donation;

import org.catalyst.model.user.Donor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A gift of goods, such as food, clothes or supplies. */
public final class InKindDonation extends Donation {
    /** Largest value that fits {@code NUMERIC(12, 3)}. */
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999.999");

    private final String itemDescription;
    private final BigDecimal quantity;
    private final String unit;
    private final String condition;

    public InKindDonation(
            Donor donor, String itemDescription, BigDecimal quantity, String unit, String condition) {
        super(donor);
        String cleanDescription = itemDescription == null ? "" : itemDescription.trim();
        String cleanUnit = unit == null ? "" : unit.trim();
        String cleanCondition = condition == null ? "" : condition.trim();

        if (cleanDescription.isEmpty() || cleanDescription.length() > 1000) {
            throw new IllegalArgumentException("Describe the items in up to 1000 characters.");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Enter a quantity greater than zero.");
        }
        if (quantity.stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException("Quantities can have at most three decimal places.");
        }
        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new IllegalArgumentException("That quantity is too large to record.");
        }
        if (cleanUnit.isEmpty() || cleanUnit.length() > 50) {
            throw new IllegalArgumentException("Enter a unit (for example boxes or kg), up to 50 characters.");
        }
        if (cleanCondition.length() > 100) {
            throw new IllegalArgumentException("The condition must be 100 characters or fewer.");
        }
        this.itemDescription = cleanDescription;
        this.quantity = quantity;
        this.unit = cleanUnit;
        this.condition = cleanCondition.isEmpty() ? null : cleanCondition;
    }

    /** Nothing to collect for goods; the donation is simply recorded. */
    @Override
    protected void execute() {
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }

    @Override
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add("Items: " + itemDescription);
        lines.add("Quantity: " + quantity.stripTrailingZeros().toPlainString() + " " + unit);
        if (condition != null) {
            lines.add("Condition: " + condition);
        }
        return lines;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    /** The condition of the items, or {@code null} when none was given. */
    public String getCondition() {
        return condition;
    }
}

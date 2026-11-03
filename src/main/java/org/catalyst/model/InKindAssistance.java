package org.catalyst.model;

public final class InKindAssistance extends AssistanceRecord {

    private String itemDescription;
    private int quantity;
    private String unit;

    public String getItemDescription() {
        return itemDescription;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setItemDetails(
            String description,
            int quantity,
            String unit) {

        String cleanDescription =
                description == null ? "" : description.trim();

        String cleanUnit = unit == null ? "" : unit.trim();

        if (cleanDescription.isBlank()) {
            throw new IllegalArgumentException(
                    "An item description is required."
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be a positive whole number."
            );
        }

        if (cleanUnit.isBlank() || cleanUnit.length() > 50) {
            throw new IllegalArgumentException(
                    "Enter a unit of 1 to 50 characters."
            );
        }

        this.itemDescription = cleanDescription;
        this.quantity = quantity;
        this.unit = cleanUnit;
    }

    @Override
    public String getSummary() {
        if (itemDescription == null) {
            return "In-kind assistance: item details not set";
        }

        return "In-kind assistance: "
                + quantity + " " + unit
                + " of " + itemDescription;
    }
}

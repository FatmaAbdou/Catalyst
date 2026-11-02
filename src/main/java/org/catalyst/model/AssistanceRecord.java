package org.catalyst.model;

import java.time.LocalDate;

public abstract class AssistanceRecord {

    private int recordID;
    private LocalDate date;
    private String category;
    private String notes = "";

    // Allows the factory to create a draft that is completed afterward.
    protected AssistanceRecord() {
    }

    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int recordID) {
        if (recordID <= 0) {
            throw new IllegalArgumentException(
                    "Record ID must be positive."
            );
        }

        if (this.recordID != 0 && this.recordID != recordID) {
            throw new IllegalStateException(
                    "An assigned record ID cannot be changed."
            );
        }

        this.recordID = recordID;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Delivery date must be today or earlier."
            );
        }

        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "An assistance category is required."
            );
        }

        this.category = category.trim();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    public abstract String getSummary();
}

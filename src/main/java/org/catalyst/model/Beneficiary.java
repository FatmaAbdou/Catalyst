package org.catalyst.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Beneficiary {

    private int beneficiaryID;
    private String name;
    private String contactInfo;
    private int householdSize;
    private String needs;

    private final List<AssistanceRecord> assistanceHistory =
            new ArrayList<>();

    public Beneficiary(
            String name,
            String contactInfo,
            int householdSize,
            String needs) {

        updateDetails(name, contactInfo, householdSize, needs);
    }

    public int getBeneficiaryID() {
        return beneficiaryID;
    }

    public void setBeneficiaryID(int beneficiaryID) {
        if (beneficiaryID <= 0) {
            throw new IllegalArgumentException(
                    "Beneficiary ID must be positive."
            );
        }

        if (this.beneficiaryID != 0
                && this.beneficiaryID != beneficiaryID) {

            throw new IllegalStateException(
                    "An assigned beneficiary ID cannot be changed."
            );
        }

        this.beneficiaryID = beneficiaryID;
    }

    public String getName() {
        return name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public int getHouseholdSize() {
        return householdSize;
    }

    public String getNeeds() {
        return needs;
    }

    public void updateDetails(
            String name,
            String contactInfo,
            int householdSize,
            String needs) {

        String cleanName = name == null ? "" : name.trim();
        String cleanContact =
                contactInfo == null ? "" : contactInfo.trim();

        if (cleanName.isBlank() || cleanName.length() > 200) {
            throw new IllegalArgumentException(
                    "Enter a beneficiary name of 1 to 200 characters."
            );
        }

        if (cleanContact.length() > 255) {
            throw new IllegalArgumentException(
                    "Contact information must be 255 characters or fewer."
            );
        }

        if (householdSize <= 0) {
            throw new IllegalArgumentException(
                    "Household size must be positive."
            );
        }

        this.name = cleanName;
        this.contactInfo = cleanContact;
        this.householdSize = householdSize;
        this.needs = needs == null ? "" : needs.trim();
    }

    public void addAssistance(AssistanceRecord record) {
        if (beneficiaryID == 0) {
            throw new IllegalStateException(
                    "Create the beneficiary before adding assistance."
            );
        }

        if (record == null) {
            throw new IllegalArgumentException(
                    "An assistance record is required."
            );
        }

        boolean duplicate = assistanceHistory.stream()
                .anyMatch(existing -> existing == record
                        || (record.getRecordID() > 0
                        && existing.getRecordID() == record.getRecordID()));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "This assistance record is already in the history."
            );
        }

        assistanceHistory.add(record);
    }

    /** Replaces the local snapshot with saved records loaded from the database. */
    public void replaceAssistanceHistory(List<AssistanceRecord> records) {
        List<AssistanceRecord> snapshot = List.copyOf(records);
        Set<Integer> recordIDs = new HashSet<>();

        for (AssistanceRecord record : snapshot) {
            if (record.getRecordID() <= 0
                    || !recordIDs.add(record.getRecordID())) {

                throw new IllegalArgumentException(
                        "History must contain saved records with unique IDs."
                );
            }
        }

        assistanceHistory.clear();
        assistanceHistory.addAll(snapshot);
    }

    public List<AssistanceRecord> getAssistanceHistory() {
        return List.copyOf(assistanceHistory);
    }
}

package org.catalyst.service;

import org.catalyst.model.AssistanceRecord;
import org.catalyst.model.Beneficiary;
import org.catalyst.model.InKindAssistance;
import org.catalyst.model.MonetaryAssistance;
import org.catalyst.model.User;
import org.catalyst.model.UserType;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BeneficiaryService {

    private final Map<Integer, Beneficiary> beneficiaries =
            new LinkedHashMap<>();

    // Associates each recorded object with exactly one beneficiary.
    private final Map<AssistanceRecord, Integer> recordOwners =
            new IdentityHashMap<>();

    private int nextBeneficiaryID = 1;
    private int nextRecordID = 1;

    public void createBeneficiary(
            User actor,
            Beneficiary beneficiary) {

        requireOfficer(actor);

        if (beneficiary == null
                || beneficiary.getBeneficiaryID() != 0) {

            throw new IllegalArgumentException(
                    "Provide a new beneficiary without an assigned ID."
            );
        }

        beneficiary.setBeneficiaryID(nextBeneficiaryID++);
        beneficiaries.put(
                beneficiary.getBeneficiaryID(),
                beneficiary
        );
    }

    public void updateBeneficiary(
            User actor,
            Beneficiary beneficiary) {

        requireOfficer(actor);

        Beneficiary stored = requireBeneficiary(beneficiary);

        // Update details without replacing existing assistance history.
        stored.updateDetails(
                beneficiary.getName(),
                beneficiary.getContactInfo(),
                beneficiary.getHouseholdSize(),
                beneficiary.getNeeds()
        );
    }

    public List<Beneficiary> searchBeneficiaries(
            User actor,
            String keyword) {

        requireOfficer(actor);

        String search = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return beneficiaries.values().stream()
                .filter(beneficiary ->
                        beneficiary.getName()
                                .toLowerCase(Locale.ROOT)
                                .contains(search)
                        || beneficiary.getContactInfo()
                                .toLowerCase(Locale.ROOT)
                                .contains(search)
                        || beneficiary.getNeeds()
                                .toLowerCase(Locale.ROOT)
                                .contains(search))
                .toList();
    }

    public void recordAssistance(
            User actor,
            Beneficiary beneficiary,
            AssistanceRecord record) {

        requireOfficer(actor);

        Beneficiary stored = requireBeneficiary(beneficiary);

        validateRecord(record);

        if (record.getRecordID() != 0
                || recordOwners.containsKey(record)) {

            throw new IllegalArgumentException(
                    "This assistance record has already been recorded."
            );
        }

        stored.addAssistance(record);
        record.setRecordID(nextRecordID++);

        recordOwners.put(record, stored.getBeneficiaryID());
    }

    public List<AssistanceRecord> viewAssistanceHistory(
            User actor,
            Beneficiary beneficiary) {

        requireOfficer(actor);

        return requireBeneficiary(beneficiary)
                .getAssistanceHistory()
                .stream()
                .sorted(
                        Comparator.comparing(AssistanceRecord::getDate)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingInt(
                                                AssistanceRecord::getRecordID
                                        ).reversed()
                                )
                )
                .toList();
    }

    public void addNote(
            User actor,
            AssistanceRecord record,
            String notes) {

        requireOfficer(actor);

        if (record == null || !recordOwners.containsKey(record)) {
            throw new IllegalArgumentException(
                    "Choose an assistance record recorded by this service."
            );
        }

        String cleanNotes = notes == null ? "" : notes.trim();

        if (cleanNotes.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter a follow-up note."
            );
        }

        String previousNotes = record.getNotes();

        record.setNotes(
                previousNotes.isBlank()
                        ? cleanNotes
                        : previousNotes
                                + System.lineSeparator()
                                + cleanNotes
        );
    }

    private Beneficiary requireBeneficiary(
            Beneficiary beneficiary) {

        Beneficiary stored = beneficiary == null
                ? null
                : beneficiaries.get(beneficiary.getBeneficiaryID());

        if (stored == null) {
            throw new IllegalArgumentException(
                    "The beneficiary has not been created in this service."
            );
        }

        return stored;
    }

    private void validateRecord(AssistanceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "An assistance record is required."
            );
        }

        if (record.getDate() == null
                || record.getCategory() == null
                || record.getCategory().isBlank()) {

            throw new IllegalArgumentException(
                    "Set the delivery date and category."
            );
        }

        if (record instanceof MonetaryAssistance monetary) {
            if (monetary.getAmount() == null) {
                throw new IllegalArgumentException(
                        "Set the monetary assistance amount."
                );
            }
        } else if (record instanceof InKindAssistance inKind) {
            if (inKind.getItemDescription() == null
                    || inKind.getUnit() == null
                    || inKind.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Set the in-kind assistance details."
                );
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported assistance record type."
            );
        }
    }

    private void requireOfficer(User actor) {
        if (actor == null
                || actor.getUserType()
                        != UserType.BENEFICIARY_OFFICER) {

            throw new SecurityException(
                    "Only beneficiary officers may manage beneficiaries."
            );
        }
    }
}

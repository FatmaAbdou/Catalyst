package org.catalyst.service;

import org.catalyst.model.AssistanceRecord;
import org.catalyst.model.Beneficiary;
import org.catalyst.model.User;
import org.catalyst.model.UserType;
import org.catalyst.repo.AssistanceRepository;
import org.catalyst.repo.BeneficiaryRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AssistanceRepository assistanceRepository;

    public BeneficiaryService() {
        this(
                new BeneficiaryRepository(),
                new AssistanceRepository()
        );
    }

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            AssistanceRepository assistanceRepository) {

        this.beneficiaryRepository =
                Objects.requireNonNull(beneficiaryRepository);

        this.assistanceRepository =
                Objects.requireNonNull(assistanceRepository);
    }

    public void createBeneficiary(
            User actor,
            Beneficiary beneficiary) throws SQLException {

        requireOfficer(actor);

        beneficiaryRepository.create(beneficiary);
    }

    public void updateBeneficiary(
            User actor,
            Beneficiary beneficiary) throws SQLException {

        requireOfficer(actor);

        // Updates beneficiary details without touching assistance records.
        beneficiaryRepository.update(beneficiary);
    }

    public List<Beneficiary> searchBeneficiaries(
            User actor,
            String keyword) throws SQLException {

        requireOfficer(actor);

        return beneficiaryRepository.search(keyword);
    }

    public void recordAssistance(
            User actor,
            Beneficiary beneficiary,
            AssistanceRecord record) throws SQLException {

        recordAssistance(actor, beneficiary, record, null);
    }

    public void recordAssistance(
            User actor,
            Beneficiary beneficiary,
            AssistanceRecord record,
            Integer campaignID) throws SQLException {

        requireOfficer(actor);
        requireBeneficiary(beneficiary);

        assistanceRepository.create(
                beneficiary.getBeneficiaryID(),
                record,
                campaignID
        );
    }

    public List<AssistanceRecord> viewAssistanceHistory(
            User actor,
            Beneficiary beneficiary) throws SQLException {

        requireOfficer(actor);
        requireBeneficiary(beneficiary);

        List<AssistanceRecord> history =
                assistanceRepository.findByBeneficiaryId(
                        beneficiary.getBeneficiaryID()
                );

        beneficiary.replaceAssistanceHistory(history);

        return history;
    }

    public void addNote(
            User actor,
            AssistanceRecord record,
            String notes) throws SQLException {

        requireOfficer(actor);

        if (record == null || record.getRecordID() <= 0) {
            throw new IllegalArgumentException(
                    "Choose a saved assistance record."
            );
        }

        String updatedNotes = assistanceRepository.appendNote(
                record.getRecordID(),
                notes
        );

        // Change the Java object only after the database update succeeds.
        record.setNotes(updatedNotes);
    }

    private void requireBeneficiary(
            Beneficiary beneficiary) throws SQLException {

        if (beneficiary == null
                || beneficiary.getBeneficiaryID() <= 0) {

            throw new IllegalArgumentException(
                    "Choose a saved beneficiary."
            );
        }

        boolean exists = beneficiaryRepository.findById(
                beneficiary.getBeneficiaryID()
        ).isPresent();

        if (!exists) {
            throw new IllegalArgumentException(
                    "The beneficiary does not exist."
            );
        }
    }

    private void requireOfficer(User actor) {
        if (actor == null || actor.getId() <= 0) {
            throw new SecurityException(
                    "Sign in with a beneficiary officer account."
            );
        }

        // Database-backed account-status checks will be added during integration.
        if (actor.getUserType() != UserType.BENEFICIARY_OFFICER) {
            throw new SecurityException(
                    "A beneficiary officer account is required."
            );
        }
    }
}

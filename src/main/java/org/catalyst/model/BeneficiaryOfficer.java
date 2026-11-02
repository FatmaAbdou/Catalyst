package org.catalyst.model;

import org.catalyst.service.BeneficiaryService;

import java.util.List;
import java.util.Objects;

public final class BeneficiaryOfficer extends User {

    private final BeneficiaryService beneficiaryService;

    // Preserves compatibility with the existing user factory.
    // This creates the account identity without connecting module operations.
    public BeneficiaryOfficer(
            String name,
            String email,
            String contactInfo) {

        super(name, email, contactInfo);
        this.beneficiaryService = null;
    }

    // Used when a service is supplied for testing or integration.
    public BeneficiaryOfficer(
            String name,
            String email,
            String contactInfo,
            BeneficiaryService beneficiaryService) {

        super(name, email, contactInfo);
        this.beneficiaryService =
                Objects.requireNonNull(beneficiaryService);
    }

    @Override
    public UserType getUserType() {
        return UserType.BENEFICIARY_OFFICER;
    }

    public void createBeneficiary(Beneficiary beneficiary) {
        service().createBeneficiary(this, beneficiary);
    }

    public void updateBeneficiary(Beneficiary beneficiary) {
        service().updateBeneficiary(this, beneficiary);
    }

    public List<Beneficiary> searchBeneficiaries(String keyword) {
        return service().searchBeneficiaries(this, keyword);
    }

    public void recordAssistance(
            Beneficiary beneficiary,
            AssistanceRecord record) {

        service().recordAssistance(this, beneficiary, record);
    }

    public List<AssistanceRecord> viewAssistanceHistory(
            Beneficiary beneficiary) {

        return service().viewAssistanceHistory(this, beneficiary);
    }

    public void addNote(AssistanceRecord record, String notes) {
        service().addNote(this, record, notes);
    }

    private BeneficiaryService service() {
        if (beneficiaryService == null) {
            throw new IllegalStateException(
                    "Beneficiary management has not been connected."
            );
        }
        return beneficiaryService;
    }
}

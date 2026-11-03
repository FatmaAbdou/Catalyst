package org.catalyst.model;

public final class HoursMilestoneCertificateFactory extends CertificateFactory {
    @Override
    public Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message) {
        if (volunteer == null || approvedHours < 1) {
            throw new IllegalArgumentException("The volunteer needs approved service hours.");
        }
        int milestone = approvedHours / 25 * 25;
        if (milestone == 0) {
            throw new IllegalArgumentException("At least 25 approved hours are needed.");
        }
        return new HoursMilestoneCertificate(
                volunteer.getName(), approvedHours, milestone + "-hour milestone");
    }
}

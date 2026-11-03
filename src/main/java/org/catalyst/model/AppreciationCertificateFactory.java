package org.catalyst.model;

public final class AppreciationCertificateFactory extends CertificateFactory {
    @Override
    public Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message) {
        String cleanMessage = message == null ? "" : message.trim();
        if (volunteer == null || cleanMessage.isBlank() || cleanMessage.length() > 500) {
            throw new IllegalArgumentException(
                    "Choose a volunteer and enter an appreciation message of up to 500 characters.");
        }
        return new AppreciationCertificate(volunteer.getName(), cleanMessage);
    }
}

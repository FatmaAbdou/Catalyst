package org.catalyst.model;

import org.catalyst.model.volunteer.Volunteer;
import org.catalyst.model.volunteer.VolunteerTask;

public final class ParticipationCertificateFactory extends CertificateFactory {
    @Override
    public Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message) {
        if (volunteer == null || task == null) {
            throw new IllegalArgumentException("Select a volunteer and a task.");
        }
        return new ParticipateCertificate(volunteer.getName(), task.description());
    }
}

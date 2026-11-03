package org.catalyst.service;

import org.catalyst.model.Certificate;
import org.catalyst.model.ParticipateCertificate;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerTask;

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

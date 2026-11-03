package org.catalyst.model;

import org.catalyst.model.volunteer.Volunteer;
import org.catalyst.model.volunteer.VolunteerTask;

/** Factory Method base for generating different volunteer certificate types. */
public abstract class CertificateFactory {
    public abstract Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message);
}

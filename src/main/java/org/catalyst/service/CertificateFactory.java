package org.catalyst.service;

import org.catalyst.model.Certificate;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerTask;

/** Factory Method base for generating different volunteer certificate types. */
public abstract class CertificateFactory {
    public abstract Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message);
}

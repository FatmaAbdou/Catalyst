package org.catalyst.model;

/** Factory Method base for generating different volunteer certificate types. */
public abstract class CertificateFactory {
    public abstract Certificate create(
            Volunteer volunteer, VolunteerTask task, int approvedHours, String message);
}

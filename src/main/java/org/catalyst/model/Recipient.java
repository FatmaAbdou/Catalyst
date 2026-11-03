package org.catalyst.model;

/** Defines the contact details and notification preferences needed to receive messages. */
public interface Recipient {
    int getId();
    String getName();
    String getEmail();
    String getPhone();
    String getSocialHandle();
    String getPreferredChannel();
    boolean isOptedIn();
}


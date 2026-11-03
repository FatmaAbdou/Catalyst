package org.catalyst.model.communication;

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


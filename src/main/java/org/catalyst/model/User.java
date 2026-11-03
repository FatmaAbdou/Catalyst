package org.catalyst.model;

import org.catalyst.communication.Recipient;

/** Shared details for the account types available on the sign-up page. */
public abstract class User implements Recipient {
    private int id;
    private final String name;
    private final String email;
    private final String contactInfo;
    private String phone;
    private String socialHandle;
    private String preferredChannel;
    private boolean optedIn = true;

    protected User(String name, String email, String contactInfo) {
        this.name = name;
        this.email = email;
        this.contactInfo = contactInfo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getSocialHandle() {
        return socialHandle;
    }

    public void setSocialHandle(String socialHandle) {
        this.socialHandle = socialHandle;
    }

    @Override
    public String getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(String channel) {
        if (channel != null && !"email".equalsIgnoreCase(channel) && !"sms".equalsIgnoreCase(channel)) {
            throw new IllegalArgumentException("Choose email or SMS.");
        }
        this.preferredChannel = channel;
    }

    @Override
    public boolean isOptedIn() {
        return optedIn;
    }

    public void setOptedIn(boolean optedIn) {
        this.optedIn = optedIn;
    }

    public abstract UserType getUserType();
}

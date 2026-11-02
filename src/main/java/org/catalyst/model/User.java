package org.catalyst.model;

/** Shared details for the account types available on the sign-up page. */
public abstract class User {
    private int id;
    private final String name;
    private final String email;
    private final String contactInfo;

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

    public abstract UserType getUserType();
}

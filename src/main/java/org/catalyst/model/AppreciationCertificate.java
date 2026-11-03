package org.catalyst.model;

/** Recognizes a volunteer's contribution with a personal message. */
public final class AppreciationCertificate extends Certificate {
    private final String message;

    public AppreciationCertificate(String volunteerName, String message) {
        super(volunteerName);
        this.message = message;
    }

    @Override
    public String getTitle() {
        return "Certificate of Appreciation";
    }

    @Override
    public String generate() {
        return buildDocument();
    }

    @Override
    protected String getMessage() {
        return message;
    }
}

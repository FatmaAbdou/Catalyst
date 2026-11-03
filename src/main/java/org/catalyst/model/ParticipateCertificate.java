package org.catalyst.model;

/** Recognizes a volunteer's participation in a task. */
public final class ParticipateCertificate extends Certificate {
    private final String taskDescription;

    public ParticipateCertificate(String volunteerName, String taskDescription) {
        super(volunteerName);
        this.taskDescription = taskDescription;
    }

    @Override
    public String getTitle() {
        return "Certificate of Participation";
    }

    @Override
    public String generate() {
        return buildDocument();
    }

    @Override
    protected String getMessage() {
        return "for contributing to " + taskDescription + " and supporting the community.";
    }
}

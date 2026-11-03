package org.catalyst.model;

/** Recognizes a volunteer's approved service-hours milestone. */
public final class HoursMilestoneCertificate extends Certificate {
    private final int totalHours;
    private final String milestone;

    public HoursMilestoneCertificate(String volunteerName, int totalHours, String milestone) {
        super(volunteerName);
        this.totalHours = totalHours;
        this.milestone = milestone;
    }

    @Override
    public String getTitle() {
        return "Service Hours Milestone";
    }

    @Override
    public String generate() {
        return buildDocument();
    }

    @Override
    protected String getMessage() {
        return "for completing " + totalHours + " approved service hours and reaching "
                + milestone + ".";
    }
}

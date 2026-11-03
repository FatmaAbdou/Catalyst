package org.catalyst.service;


import org.catalyst.model.Event.RegistrationStatus;
import org.catalyst.model.Event.Ticket;

public final class Registration {
    private final int id;
    private final int userId;
    private final int eventId;
    private RegistrationStatus status;
    private final String paymentReference;
    private boolean attended;
    private Ticket ticket;

    Registration(int id, int userId, int eventId,
                 RegistrationStatus status, String paymentReference) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.status = status;
        this.paymentReference = paymentReference;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getEventId() { return eventId; }
    public RegistrationStatus getStatus() { return status; }
    public String getPaymentReference() { return paymentReference; }
    public boolean isAttended() { return attended; }
    public Ticket getTicket() { return ticket; }

    void setStatus(RegistrationStatus status) { this.status = status; }
    void setTicket(Ticket ticket) { this.ticket = ticket; }
}

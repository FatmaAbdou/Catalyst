package org.catalyst.model.communication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Holds the message content, recipients, and creation time. */
public class Message {
    private int id;
    private String subject;
    private String body;
    private List<Recipient> recipients;
    private String createdAt;

    public Message(int id, String subject, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body cannot be empty.");
        }
        this.id = id;
        this.subject = subject == null ? "" : subject;
        this.body = body;
        this.recipients = new ArrayList<>();
        this.createdAt = LocalDateTime.now().toString();
    }

    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public List<Recipient> getRecipients() {
        return new ArrayList<>(recipients);
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void addRecipient(Recipient recipient) {
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient cannot be null.");
        }
        for (Recipient existing : recipients) {
            if (existing == recipient || (existing.getId() > 0
                    && existing.getId() == recipient.getId())) {
                return;
            }
        }
        recipients.add(recipient);
    }
}


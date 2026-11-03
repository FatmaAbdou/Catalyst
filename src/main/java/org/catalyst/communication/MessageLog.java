package org.catalyst.communication;

import java.util.ArrayList;
import java.util.List;

/** Keeps message contents and sending results in memory for history lookup. */
public class MessageLog {
    private List<DeliveryResult> entries = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();

    public void record(Message message, String channel, String status, String error) {
        if (message == null || message.getRecipients().size() > 1) {
            throw new IllegalArgumentException("Record one recipient or one public post at a time.");
        }
        Recipient recipient = message.getRecipients().isEmpty() ? null : message.getRecipients().get(0);
        Message savedMessage = new Message(message.getId(), message.getSubject(), message.getBody());
        if (recipient != null) {
            savedMessage.addRecipient(recipient);
        }
        entries.add(new DeliveryResult(status, channel, recipient, error));
        messages.add(savedMessage);
    }

    public List<DeliveryResult> getHistory(int recipientId) {
        List<DeliveryResult> history = new ArrayList<>();
        for (DeliveryResult entry : entries) {
            if (recipientId > 0 && entry.getRecipientId() == recipientId) {
                history.add(entry);
            }
        }
        return history;
    }

    public List<DeliveryResult> getByStatus(String status) {
        List<DeliveryResult> matches = new ArrayList<>();
        for (DeliveryResult entry : entries) {
            if (entry.getStatus().equalsIgnoreCase(status)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}


package org.catalyst.communication;

import java.time.LocalDateTime;

/** Stores the status, recipient, time, and error details of a sending attempt. */
public class DeliveryResult {
    private String status;
    private String channel;
    private String recipient;
    private int recipientId;
    private String timestamp;
    private String errorMessage;

    public DeliveryResult(String status, String channel, Recipient recipient, String errorMessage) {
        this.status = status;
        this.channel = channel;
        this.recipient = recipient == null ? "" : recipient.getName();
        this.recipientId = recipient == null ? 0 : recipient.getId();
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getStatus() {
        return status;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return "SIMULATED".equals(status);
    }
}


package org.catalyst.communication;

import java.util.List;

/** Coordinates message sending through the selected strategy and records the results. */
public class CommunicationService {
    private CommunicationStrategy strategy;
    private RecipientDirectory directory;
    private MessageLog log;
    private int nextMessageId = 1;

    public CommunicationService(RecipientDirectory directory, MessageLog log) {
        if (directory == null || log == null) {
            throw new IllegalArgumentException("Recipient directory and message log are required.");
        }
        this.directory = directory;
        this.log = log;
    }

    public void setStrategy(CommunicationStrategy strategy) {
        this.strategy = strategy;
    }

    public DeliveryResult send(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        if (strategy != null && !strategy.requiresRecipient()) {
            DeliveryResult result = strategy.send(message);
            if (message.getRecipients().isEmpty()) {
                log.record(message, result.getChannel(), result.getStatus(), result.getErrorMessage());
            }
            return result;
        }
        List<Recipient> recipients = message.getRecipients();
        if (recipients.isEmpty()) {
            return new DeliveryResult("FAILED", "", null, "Select at least one recipient.");
        }

        int successes = 0;
        DeliveryResult lastResult = null;
        for (Recipient recipient : recipients) {
            Message individualMessage = new Message(message.getId(), message.getSubject(), message.getBody());
            individualMessage.addRecipient(recipient);
            CommunicationStrategy selected = strategy;
            if (selected == null) {
                selected = preferredStrategy(recipient);
            }

            DeliveryResult result;
            if (selected == null) {
                result = new DeliveryResult("FAILED", "", recipient, "Set a preferred channel or select a strategy.");
            } else {
                result = selected.send(individualMessage);
            }
            log.record(individualMessage, result.getChannel(), result.getStatus(), result.getErrorMessage());
            if (result.isSuccess()) {
                successes++;
            }
            lastResult = result;
        }
        if (recipients.size() == 1) {
            return lastResult;
        }
        String status;
        if (successes == recipients.size()) {
            status = "SIMULATED";
        } else if (successes == 0) {
            status = "FAILED";
        } else {
            status = "PARTIAL";
        }
        String channel = strategy == null ? "preferred" : strategy.getChannel();
        String error = successes == recipients.size() ? null
                : successes + " of " + recipients.size() + " messages were simulated successfully.";
        return new DeliveryResult(status, channel, null, error);
    }

    public DeliveryResult sendToRecipient(int recipientId, String subject, String body) {
        Recipient recipient = directory.findById(recipientId);
        if (recipient == null) {
            return new DeliveryResult("FAILED", "", null, "Recipient was not found.");
        }
        Message message = new Message(nextMessageId++, subject, body);
        message.addRecipient(recipient);
        return send(message);
    }

    public DeliveryResult broadcast(String role, String subject, String body) {
        if (strategy != null && !strategy.requiresRecipient()) {
            return new DeliveryResult("FAILED", strategy.getChannel(), null,
                    "Use send() with no recipients to publish on the organization's page.");
        }
        Message message = new Message(nextMessageId++, subject, body);
        for (Recipient recipient : directory.findByRole(role)) {
            message.addRecipient(recipient);
        }
        return send(message);
    }

    private CommunicationStrategy preferredStrategy(Recipient recipient) {
        String channel = recipient.getPreferredChannel();
        if ("email".equalsIgnoreCase(channel)) {
            return new EmailChannel();
        }
        if ("sms".equalsIgnoreCase(channel)) {
            return new SMSChannel();
        }
        return null;
    }
}


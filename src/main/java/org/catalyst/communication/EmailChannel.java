package org.catalyst.communication;

/** Validates the recipient's email address and simulates sending an email. */
public class EmailChannel implements CommunicationStrategy {
    @Override
    public boolean requiresRecipient() {
        return true;
    }

    @Override
    public String getChannel() {
        return "email";
    }

    @Override
    public boolean supports(Recipient recipient) {
        return recipient != null && validateAddress(recipient.getEmail());
    }

    private boolean validateAddress(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    @Override
    public DeliveryResult send(Message message) {
        if (message == null || message.getRecipients().size() != 1) {
            return new DeliveryResult("FAILED", getChannel(), null, "Select one recipient per send.");
        }
        Recipient recipient = message.getRecipients().get(0);
        if (!recipient.isOptedIn()) {
            return new DeliveryResult("FAILED", getChannel(), recipient, "Recipient has opted out.");
        }
        if (!supports(recipient)) {
            return new DeliveryResult("FAILED", getChannel(), recipient, "A valid email address is required.");
        }
        System.out.println("[DEMO EMAIL] To: " + recipient.getEmail());
        System.out.println("Subject: " + message.getSubject());
        System.out.println(message.getBody());
        return new DeliveryResult("SIMULATED", getChannel(), recipient, null);
    }
}


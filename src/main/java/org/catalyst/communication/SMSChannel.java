package org.catalyst.communication;

/** Validates the recipient's phone number and simulates sending an SMS. */
public class SMSChannel implements CommunicationStrategy {
    @Override
    public String getChannel() {
        return "sms";
    }

    @Override
    public boolean supports(Recipient recipient) {
        return recipient != null && recipient.getPhone() != null
                && recipient.getPhone().matches("\\+?[0-9]{7,15}");
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
            return new DeliveryResult("FAILED", getChannel(), recipient, "A valid phone number is required.");
        }
        System.out.println("[DEMO SMS] To: " + recipient.getPhone());
        System.out.println(message.getBody());
        return new DeliveryResult("SIMULATED", getChannel(), recipient, null);
    }
}


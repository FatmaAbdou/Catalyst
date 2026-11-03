package org.catalyst.model.communication;

/** Defines the operations shared by all communication channels. */
public interface CommunicationStrategy {
    DeliveryResult send(Message message);
    boolean supports(Recipient recipient);
    boolean requiresRecipient();
    String getChannel();
}


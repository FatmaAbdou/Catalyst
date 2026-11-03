package org.catalyst.model;

/** Simulates publishing a caption on the organization's Instagram account. */
public class InstagramChannel extends SocialMediaChannel {
    public InstagramChannel(String account) {
        super(account);
    }

    @Override
    public String getChannel() {
        return "instagram";
    }

    @Override
    public DeliveryResult send(Message message) {
        if (message == null || !message.getRecipients().isEmpty()) {
            return new DeliveryResult("FAILED", getChannel(), null,
                    "Public posts must not contain personal recipients.");
        }
        if (!hasAccount()) {
            return new DeliveryResult("FAILED", getChannel(), null, "Set the organization's Instagram account.");
        }
        publish(formatPost(message));
        return new DeliveryResult("SIMULATED", getChannel(), null, null);
    }

    @Override
    protected String formatPost(Message message) {
        return message.getBody();
    }

    @Override
    protected void publish(String post) {
        System.out.println("[DEMO INSTAGRAM] Account: " + account);
        System.out.println("Caption: " + post);
    }
}


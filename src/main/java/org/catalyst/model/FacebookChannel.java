package org.catalyst.model;

/** Simulates publishing a post on the organization's Facebook page. */
public class FacebookChannel extends SocialMediaChannel {
    public FacebookChannel(String account) {
        super(account);
    }

    @Override
    public String getChannel() {
        return "facebook";
    }

    @Override
    public DeliveryResult send(Message message) {
        if (message == null || !message.getRecipients().isEmpty()) {
            return new DeliveryResult("FAILED", getChannel(), null,
                    "Public posts must not contain personal recipients.");
        }
        if (!hasAccount()) {
            return new DeliveryResult("FAILED", getChannel(), null, "Set the organization's Facebook page.");
        }
        publish(formatPost(message));
        return new DeliveryResult("SIMULATED", getChannel(), null, null);
    }

    @Override
    protected String formatPost(Message message) {
        return message.getSubject().isBlank() ? message.getBody()
                : message.getSubject() + "\n" + message.getBody();
    }

    @Override
    protected void publish(String post) {
        System.out.println("[DEMO FACEBOOK] Page: " + account);
        System.out.println(post);
    }
}


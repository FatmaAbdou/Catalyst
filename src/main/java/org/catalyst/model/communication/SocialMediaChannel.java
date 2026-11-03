package org.catalyst.model.communication;

/** Shares account details and posting operations between social-media strategies. */
public abstract class SocialMediaChannel implements CommunicationStrategy {
    protected String account;

    protected SocialMediaChannel(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    @Override
    public boolean requiresRecipient() {
        return false;
    }

    @Override
    public boolean supports(Recipient recipient) {
        return recipient == null && hasAccount();
    }

    protected boolean hasAccount() {
        return account != null && !account.isBlank();
    }

    protected abstract String formatPost(Message message);
    protected abstract void publish(String post);
}


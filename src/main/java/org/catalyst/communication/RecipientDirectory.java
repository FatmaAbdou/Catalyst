package org.catalyst.communication;

import org.catalyst.model.User;
import java.util.ArrayList;
import java.util.List;

/** Keeps recipients in memory and finds them by ID or user role. */
public class RecipientDirectory {
    private List<Recipient> recipients = new ArrayList<>();

    public void addRecipient(Recipient recipient) {
        if (recipient == null || recipient.getId() <= 0) {
            throw new IllegalArgumentException("Recipient must have a positive ID.");
        }
        if (findById(recipient.getId()) != null) {
            throw new IllegalArgumentException("A recipient with this ID already exists.");
        }
        recipients.add(recipient);
    }

    public Recipient findById(int id) {
        for (Recipient recipient : recipients) {
            if (recipient.getId() == id) {
                return recipient;
            }
        }
        return null;
    }

    public List<Recipient> findByRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty.");
        }
        List<Recipient> matches = new ArrayList<>();
        for (Recipient recipient : recipients) {
            if (recipient instanceof User) {
                User user = (User) recipient;
                if (user.getUserType().name().equalsIgnoreCase(role)) {
                    matches.add(recipient);
                }
            }
        }
        return matches;
    }

    public List<Recipient> findAll() {
        return new ArrayList<>(recipients);
    }
}


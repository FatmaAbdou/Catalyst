package org.catalyst.model;


import java.time.Instant;
import java.util.UUID;

public final class Ticket {
    private final String uniqueReference = UUID.randomUUID().toString();
    private final Instant issuedAt = Instant.now();
    private boolean valid = true;

    public String getUniqueReference() { return uniqueReference; }
    public Instant getIssuedAt() { return issuedAt; }
    public boolean isValid() { return valid; }

    void invalidate() { valid = false; }
}

package org.catalyst.model;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Base type for printable volunteer certificates. */
public abstract class Certificate {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id = NEXT_ID.getAndIncrement();
    private final LocalDate issueDate = LocalDate.now();
    private final String volunteerName;

    protected Certificate(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public abstract String generate();

    protected final String buildDocument() {
        return """
                <!doctype html>
                <html><head><meta charset="utf-8">
                <title>Certificate</title>
                <style>
                body { font-family: Georgia, serif; color: #214e3d; background: #f3f5ef; }
                main { max-width: 760px; margin: 60px auto; padding: 64px; text-align: center;
                       border: 8px double #4a8069; background: #fffefa; }
                h1 { font-size: 38px; } h2 { color: #b87543; }
                p { font-size: 20px; } footer { margin-top: 48px; color: #68766d; }
                </style></head><body><main>
                <p>Catalyst · Community support</p>
                <h1>Catalyst Volunteer Certificate</h1>
                <h2>%s</h2>
                <p>This certificate recognizes %s</p>
                <p>%s</p>
                <footer>Certificate #%d · Issued %s</footer>
                </main></body></html>
                """.formatted(
                escape(getTitle()),
                escape(volunteerName),
                escape(getMessage()),
                id,
                issueDate);
    }

    public abstract String getTitle();

    protected abstract String getMessage();

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

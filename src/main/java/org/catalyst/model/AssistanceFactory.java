package org.catalyst.model;

import java.util.Locale;

public final class AssistanceFactory {

    public AssistanceRecord createAssistance(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Choose an assistance type."
            );
        }

        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "MONETARY" -> new MonetaryAssistance();
            case "IN_KIND" -> new InKindAssistance();
            default -> throw new IllegalArgumentException(
                    "Unsupported assistance type: " + type
            );
        };
    }
}

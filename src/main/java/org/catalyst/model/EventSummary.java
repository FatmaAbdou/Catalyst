package org.catalyst.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventSummary(
        int eventId,
        String name,
        String location,
        LocalDateTime startsAt,
        BigDecimal ticketPrice
) {}
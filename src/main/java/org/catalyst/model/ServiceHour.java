package org.catalyst.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Hours recorded against a volunteer assignment. */
public record ServiceHour(LocalDate date, BigDecimal hours, String taskDescription) {

    @Override
    public String toString() {
        return date + "  ·  " + hours.stripTrailingZeros().toPlainString()
                + " hours  ·  " + taskDescription;
    }
}

package org.catalyst.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Hours recorded against a volunteer assignment. */
public record ServiceHour(
        int id,
        int assignmentId,
        String volunteerName,
        LocalDate date,
        BigDecimal hours,
        String taskDescription,
        String status) {

    @Override
    public String toString() {
        String volunteer = volunteerName == null ? "" : volunteerName + " · ";
        return volunteer + date + " · " + hours.stripTrailingZeros().toPlainString()
                + " hours · " + taskDescription + " · " + status;
    }
}

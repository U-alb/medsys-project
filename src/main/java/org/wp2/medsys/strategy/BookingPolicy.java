package org.wp2.medsys.strategy;

import java.time.LocalDateTime;

public interface BookingPolicy {
    /**
     * Throws BookingConflictException if the booking is not allowed for the given doctor/slot.
     */
    void assertCanBook(Long doctorId, LocalDateTime when);

    /**
     * Human-readable policy name for logs.
     */
    String name();
}

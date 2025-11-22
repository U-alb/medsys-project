package org.wp2.medsys.appointmentsservice.errors;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }
}

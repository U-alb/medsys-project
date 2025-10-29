package org.wp2.medsys.errors;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

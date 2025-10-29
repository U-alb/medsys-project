package org.wp2.medsys.validation;

import org.wp2.medsys.domain.Appointment;

public interface ValidationHandler {
    void setNext(ValidationHandler next);
    void handle(Appointment appointment) throws Throwable;
}

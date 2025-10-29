package org.wp2.medsys.validation;

import org.wp2.medsys.domain.Appointment;

public abstract class BaseValidationHandler implements ValidationHandler {
    private ValidationHandler next;

    @Override
    public final void setNext(ValidationHandler next) {
        this.next = next;
    }

    @Override
    public final void handle(Appointment appointment) throws Throwable {
        doValidate(appointment);
        if (next != null) next.handle(appointment);
    }

    protected abstract void doValidate(Appointment appointment) throws Throwable;
}

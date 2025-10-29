package org.wp2.medsys.notify;

import org.wp2.medsys.domain.Appointment;

public interface Notifier {
    void onAppointmentScheduled(Appointment appt);
    void onAppointmentDecided(Appointment appt);
}

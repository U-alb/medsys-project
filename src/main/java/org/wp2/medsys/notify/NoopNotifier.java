package org.wp2.medsys.notify;

import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.domain.Appointment;

@Slf4j
public class NoopNotifier implements Notifier {
    @Override
    public void onAppointmentScheduled(Appointment appt) {
        log.debug("[Notifier=None] Skipping scheduled notification. apptId={}", appt.getId());
    }

    @Override
    public void onAppointmentDecided(Appointment appt) {
        log.debug("[Notifier=None] Skipping decision notification. apptId={}", appt.getId());
    }
}

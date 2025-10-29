package org.wp2.medsys.notify;

import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.domain.Doctor;
import org.wp2.medsys.domain.Patient;

@Slf4j
public class EmailNotifier implements Notifier {

    @Override
    public void onAppointmentScheduled(Appointment appt) {
        Doctor d = appt.getDoctor();
        Patient p = appt.getPatient();
        log.info("[Notifier=Email] Email sent: Scheduled {} with Dr. {} ({}) for patient {} ({}) at {}",
                appt.getId(),
                d != null ? d.getUsername() : "n/a",
                d != null ? d.getEmail()    : "n/a",
                p != null ? p.getUsername() : "n/a",
                p != null ? p.getEmail()    : "n/a",
                appt.getAppointmentDate()
        );
    }

    @Override
    public void onAppointmentDecided(Appointment appt) {
        Doctor d = appt.getDoctor();
        Patient p = appt.getPatient();
        log.info("[Notifier=Email] Email sent: Decision {} for appt {} (doctor={}, patient={}, status={})",
                "notification",
                appt.getId(),
                d != null ? d.getUsername() : "n/a",
                p != null ? p.getUsername() : "n/a",
                appt.getStatus()
        );
    }
}

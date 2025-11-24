package org.wp2.medsys.services;

import org.wp2.medsys.domain.Appointment;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    List<Appointment> findAll();
    Optional<Appointment> findById(Long id);
    Appointment create(Appointment appointment) throws Throwable;
    Appointment update(Appointment appointment);
    void deleteById(Long id);

    void deleteAll();

    /**
     * Orchestrated decision (doctor side):
     *  - Verify caller is DOCTOR and owns the appointment
     *  - Only PENDING -> ACCEPTED/DENIED transitions allowed
     *  - Persist and Notifier.onAppointmentDecided(...)
     *  - Returns the updated appointment
     *
     * @param appointmentId ID of appointment to decide
     * @param decision      "ACCEPT" or "DENY" (also accepts "ACCEPTED"/"DENIED")
     */
    Appointment decide(Long appointmentId, String decision) throws Throwable;
}

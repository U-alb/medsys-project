package org.wp2.medsys.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.errors.ConflictException;
import org.wp2.medsys.repositories.AppointmentRepository;

@Slf4j
@RequiredArgsConstructor
public class PatientConflictCheck extends BaseValidationHandler {

    private final AppointmentRepository appointmentRepository;

    @Override
    protected void doValidate(Appointment appointment) {
        Long patientId = appointment.getPatient().getId();
        long existing = appointmentRepository.countByPatientIdAndAppointmentDate(
                patientId, appointment.getAppointmentDate()
        );
        if (existing > 0) {
            log.info("[CoR] PatientConflictCheck -> FAIL (patient overlap) patientId={}, when={}",
                    patientId, appointment.getAppointmentDate());
            throw new ConflictException("You already have an appointment at this time.");
        }
        log.info("[CoR] PatientConflictCheck -> OK patientId={}, when={}", patientId, appointment.getAppointmentDate());
    }
}

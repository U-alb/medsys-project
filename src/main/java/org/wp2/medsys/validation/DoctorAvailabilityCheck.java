package org.wp2.medsys.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.errors.BadRequestException;
import org.wp2.medsys.repositories.DoctorRepository;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class DoctorAvailabilityCheck extends BaseValidationHandler {

    private final DoctorRepository doctorRepository;

    @Override
    protected void doValidate(Appointment appointment) throws Throwable {
        Long doctorId = appointment.getDoctor().getId();
        if (doctorId == null || !doctorRepository.existsById(doctorId)) {
            log.info("[CoR] DoctorAvailabilityCheck -> FAIL (doctor missing)");
            throw new Throwable("Doctor does not exist.");
        }

        LocalDateTime when = appointment.getAppointmentDate();
        if (when == null) {
            log.info("[CoR] DoctorAvailabilityCheck -> FAIL (no datetime)");
            throw new Throwable("Appointment date/time must be set.");
        }
        if (when.isBefore(LocalDateTime.now())) {
            log.info("[CoR] DoctorAvailabilityCheck -> FAIL (past datetime)");
            throw new Throwable("Cannot schedule in the past.");
        }

        log.info("[CoR] DoctorAvailabilityCheck -> OK doctorId={}, when={}", doctorId, when);
    }
}

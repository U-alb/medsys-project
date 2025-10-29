package org.wp2.medsys.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.errors.ConflictException;
import org.wp2.medsys.repositories.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class BusinessRulesCheck extends BaseValidationHandler {

    private final AppointmentRepository appointmentRepository;

    @Value("${medsys.rules.max-per-day:3}")
    private int maxPerDay;

    @Override
    protected void doValidate(Appointment appointment) {
        Long patientId = appointment.getPatient().getId();
        LocalDate day = appointment.getAppointmentDate().toLocalDate();

        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay().minusNanos(1);

        long count = appointmentRepository.countByPatientIdAndAppointmentDateBetween(patientId, start, end);
        if (count >= maxPerDay) {
            log.info("[CoR] BusinessRulesCheck -> FAIL (max-per-day) patientId={}, day={}, count={}, limit={}",
                    patientId, day, count, maxPerDay);
            throw new ConflictException("Daily limit reached for appointments.");
        }

        log.info("[CoR] BusinessRulesCheck -> OK patientId={}, day={}, count={}, limit={}",
                patientId, day, count, maxPerDay);
    }
}

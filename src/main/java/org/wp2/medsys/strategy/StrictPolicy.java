package org.wp2.medsys.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.repositories.AppointmentRepository;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class StrictPolicy implements BookingPolicy {

    private final AppointmentRepository appointmentRepository;

    @Override
    public void assertCanBook(Long doctorId, LocalDateTime when) {
        long existing = appointmentRepository.countByDoctorIdAndAppointmentDate(doctorId, when);
        log.debug("[BookingPolicy=Strict] doctorId={}, when={}, existing={}", doctorId, when, existing);
        if (existing >= 1) {
            throw new BookingConflictException("Slot already taken for this doctor at the requested time.");
        }
    }

    @Override
    public String name() {
        return "strict";
    }
}

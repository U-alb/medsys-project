package org.wp2.medsys.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.wp2.medsys.repositories.AppointmentRepository;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class BufferedOverbookPolicy implements BookingPolicy {

    private final AppointmentRepository appointmentRepository;
    private final int buffer;

    @Override
    public void assertCanBook(Long doctorId, LocalDateTime when) {
        long existing = appointmentRepository.countByDoctorIdAndAppointmentDate(doctorId, when);
        int capacity = 1 + Math.max(0, buffer); // base 1 + allowed overlaps
        log.debug("[BookingPolicy=Buffered] doctorId={}, when={}, existing={}, capacity={}", doctorId, when, existing, capacity);
        if (existing >= capacity) {
            throw new BookingConflictException("Overbooking limit reached for this time slot.");
        }
    }

    @Override
    public String name() {
        return "buffered(" + buffer + ")";
    }
}

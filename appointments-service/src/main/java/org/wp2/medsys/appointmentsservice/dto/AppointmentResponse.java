package org.wp2.medsys.appointmentsservice.dto;

import org.wp2.medsys.appointmentsservice.domain.Status;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        String patientUsername,
        String doctorUsername,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Status status,
        String scheduleReason
) {}

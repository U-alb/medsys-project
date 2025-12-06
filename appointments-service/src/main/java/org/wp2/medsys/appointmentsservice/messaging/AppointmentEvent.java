package org.wp2.medsys.appointmentsservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload sent from appointments-service to other services via RabbitMQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEvent {

    private AppointmentEventType type;

    private Long appointmentId;

    private String patientUsername;

    private String doctorUsername;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String scheduleReason;
}

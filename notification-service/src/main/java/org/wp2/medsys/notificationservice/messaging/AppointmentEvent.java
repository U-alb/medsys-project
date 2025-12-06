package org.wp2.medsys.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload as sent by appointments-service through RabbitMQ.
 * Field names MUST match the publisher.
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

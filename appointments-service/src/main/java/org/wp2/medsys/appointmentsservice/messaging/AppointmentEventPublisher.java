package org.wp2.medsys.appointmentsservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.wp2.medsys.appointmentsservice.config.RabbitConfig;
import org.wp2.medsys.appointmentsservice.domain.Appointment;

@Service
@RequiredArgsConstructor
@Slf4j
@Component
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(Appointment appointment) {
        publish(appointment, AppointmentEventType.APPOINTMENT_CREATED);
    }

    public void publishAccepted(Appointment appointment) {
        publish(appointment, AppointmentEventType.APPOINTMENT_ACCEPTED);
    }

    public void publishRejected(Appointment appointment) {
        publish(appointment, AppointmentEventType.APPOINTMENT_REJECTED);
    }

    public void publishCancelled(Appointment appointment) {
        publish(appointment, AppointmentEventType.APPOINTMENT_CANCELLED);
    }

    private void publish(Appointment appointment, AppointmentEventType type) {
        if (appointment == null || appointment.getId() == null) {
            log.warn("Skipping publish of {} event: appointment or id is null", type);
            return;
        }

        AppointmentEvent event = new AppointmentEvent(
                type,
                appointment.getId(),
                appointment.getPatientUsername(),
                appointment.getDoctorUsername(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getScheduleReason()
        );

        log.info("Publishing appointment event: type={}, id={}, patient={}, doctor={}",
                type, appointment.getId(),
                appointment.getPatientUsername(),
                appointment.getDoctorUsername()
        );

        rabbitTemplate.convertAndSend(
                RabbitConfig.APPOINTMENTS_EXCHANGE,
                RabbitConfig.NOTIFICATIONS_ROUTING_KEY,
                event
        );
    }
}

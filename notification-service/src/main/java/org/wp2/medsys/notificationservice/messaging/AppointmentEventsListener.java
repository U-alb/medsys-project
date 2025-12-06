package org.wp2.medsys.notificationservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.wp2.medsys.notificationservice.config.RabbitConfig;
import org.wp2.medsys.notificationservice.services.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventsListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATIONS_QUEUE)
    public void handleAppointmentEvent(AppointmentEvent event) {
        log.info("Received appointment event: type={}, appointmentId={}, patient={}, doctor={}",
                event.getType(), event.getAppointmentId(),
                event.getPatientUsername(), event.getDoctorUsername());

        notificationService.createFromAppointmentEvent(event);

        log.info("Notification persisted for patient {}", event.getPatientUsername());
    }
}

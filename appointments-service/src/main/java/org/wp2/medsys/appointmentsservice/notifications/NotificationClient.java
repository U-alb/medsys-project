package org.wp2.medsys.appointmentsservice.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.wp2.medsys.appointmentsservice.domain.Appointment;

@Slf4j
@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationClient(RestTemplateBuilder restTemplateBuilder,
                              @Value("${notifications.base-url:http://localhost:8083}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = baseUrl;
    }

    /* ------------ public helpers for appointments ------------ */

    public void sendAppointmentAccepted(Appointment appointment) {
        NotificationCreateRequest request = new NotificationCreateRequest(
                appointment.getPatientUsername(),
                "Appointment accepted",
                "Your appointment with doctor " + appointment.getDoctorUsername() +
                        " on " + appointment.getStartTime() + " was accepted.",
                "APPOINTMENT_ACCEPTED",
                appointment.getId()
        );
        postNotification(request);
    }

    public void sendAppointmentDenied(Appointment appointment) {
        NotificationCreateRequest request = new NotificationCreateRequest(
                appointment.getPatientUsername(),
                "Appointment rejected",
                "Your appointment with doctor " + appointment.getDoctorUsername() +
                        " on " + appointment.getStartTime() + " was rejected.",
                "APPOINTMENT_DENIED",
                appointment.getId()
        );
        postNotification(request);
    }

    public void sendAppointmentCancelled(Appointment appointment) {
        // patient cancelled → notify doctor
        NotificationCreateRequest request = new NotificationCreateRequest(
                appointment.getDoctorUsername(),
                "Appointment cancelled",
                "Appointment with patient " + appointment.getPatientUsername() +
                        " on " + appointment.getStartTime() + " was cancelled by the patient.",
                "APPOINTMENT_CANCELLED",
                appointment.getId()
        );
        postNotification(request);
    }

    /* ------------ internal HTTP helper ------------ */

    private void postNotification(NotificationCreateRequest request) {
        String url = baseUrl + "/notifications/internal/create";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NotificationCreateRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            // Important: DO NOT fail the appointment just because notifications are down.
            log.warn("Failed to send notification for appointment {}: {}",
                    request.relatedAppointmentId(), e.getMessage());
        }
    }
}

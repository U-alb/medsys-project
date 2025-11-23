package org.wp2.medsys.appointmentsservice.notifications;

/**
 * DTO sent from appointments-service to notification-service.
 * Mirrors notification-service NotificationCreateDTO.
 */
public record NotificationCreateRequest(
        String recipientUsername,
        String title,
        String message,
        String type,
        Long relatedAppointmentId
) {
}

package org.wp2.medsys.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload used when creating a new notification.
 * Can be sent by other services (appointments-service)
 * or directly via Postman.
 */
public record NotificationCreateDTO(

        @NotBlank
        @Size(max = 100)
        String recipientUsername,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 1000)
        String message,

        // e.g. "APPOINTMENT_CREATED", "APPOINTMENT_ACCEPTED", etc.
        @NotBlank
        String type,

        Long relatedAppointmentId
) {}

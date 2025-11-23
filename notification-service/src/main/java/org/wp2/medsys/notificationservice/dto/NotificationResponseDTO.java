package org.wp2.medsys.notificationservice.dto;

import org.wp2.medsys.notificationservice.domain.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        String recipientUsername,
        String title,
        String message,
        org.wp2.medsys.notificationservice.domain.NotificationType type,
        NotificationStatus status,
        Long relatedAppointmentId,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

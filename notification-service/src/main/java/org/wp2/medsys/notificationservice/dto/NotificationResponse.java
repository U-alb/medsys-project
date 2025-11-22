package org.wp2.medsys.notificationservice.dto;

import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String recipientUsername,
        String title,
        String message,
        NotificationType type,
        Long relatedAppointmentId,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}

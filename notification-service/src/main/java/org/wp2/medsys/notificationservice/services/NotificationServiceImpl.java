package org.wp2.medsys.notificationservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.domain.NotificationType;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;
import org.wp2.medsys.notificationservice.repositories.NotificationRepository;
import org.wp2.medsys.notificationservice.messaging.AppointmentEvent;
import org.wp2.medsys.notificationservice.messaging.AppointmentEventType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Default implementation of NotificationService.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification create(NotificationCreateDTO dto) {
        Notification notification = new Notification();

        // Adapt these to your actual entity field names
        notification.setRecipientUsername(dto.recipientUsername());
        notification.setTitle(dto.title());
        notification.setMessage(dto.message());
        notification.setType(NotificationType.valueOf(dto.type()));
        notification.setRelatedAppointmentId(dto.relatedAppointmentId());

        notification.setStatus(NotificationStatus.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> findAll() {
        // You can change this to whatever ordering you prefer
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> findForRecipient(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public List<Notification> findForRecipient(String username, NotificationStatus status) {
        return notificationRepository.findByRecipientUsernameAndStatusOrderByCreatedAtDesc(username, status);
    }

    @Override
    public long countUnread(String username) {
        return notificationRepository.countByRecipientUsernameAndStatus(
                username,
                NotificationStatus.UNREAD
        );
    }

    @Override
    public Notification markAsRead(Long id, String username) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUsername(id, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found or does not belong to user"
                ));

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification = notificationRepository.save(notification);
        }

        return notification;
    }

    @Override
    public int markAllAsRead(String username) {
        List<Notification> unread = notificationRepository
                .findByRecipientUsernameAndStatus(username, NotificationStatus.UNREAD);

        if (unread.isEmpty()) {
            return 0;
        }

        unread.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unread);

        return unread.size();
    }

    @Override
    public Notification createNotification(String username, String message) {
        // derive a short title from the message (max 100 chars)
        String baseTitle = message != null ? message.trim() : "";
        if (baseTitle.isEmpty()) {
            baseTitle = "Notification";
        }
        String title = baseTitle.length() > 100 ? baseTitle.substring(0, 100) : baseTitle;

        // simple generic type for now; can specialize later
        String type = "GENERIC";

        NotificationCreateDTO dto = new NotificationCreateDTO(
                username,
                title,
                message,
                type,
                null  // no related appointment by default
        );

        return create(dto);
    }

    @Override
    public Notification createFromAppointmentEvent(AppointmentEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        AppointmentEventType eventType = event.getType();
        if (eventType == null) {
            throw new IllegalArgumentException("event.type must not be null");
        }

        String patient = event.getPatientUsername();
        String doctor = event.getDoctorUsername();
        String when = formatInterval(event.getStartTime(), event.getEndTime());

        NotificationType type;
        String title;
        String message;

        switch (eventType) {
            case APPOINTMENT_CREATED -> {
                type = NotificationType.APPOINTMENT_CREATED;
                title = "Appointment requested";
                message = String.format(
                        "You requested an appointment with doctor %s on %s.",
                        doctor, when
                );
            }
            case APPOINTMENT_ACCEPTED -> {
                type = NotificationType.APPOINTMENT_ACCEPTED;
                title = "Appointment accepted";
                message = String.format(
                        "Your appointment with doctor %s on %s was accepted.",
                        doctor, when
                );
            }
            case APPOINTMENT_REJECTED -> {
                // We map REJECTED events to the existing DENIED notification type
                type = NotificationType.APPOINTMENT_DENIED;
                title = "Appointment rejected";
                message = String.format(
                        "Your appointment request with doctor %s on %s was rejected.",
                        doctor, when
                );
            }
            case APPOINTMENT_CANCELLED -> {
                type = NotificationType.APPOINTMENT_CANCELLED;
                title = "Appointment cancelled";
                message = String.format(
                        "Your appointment with doctor %s on %s was cancelled.",
                        doctor, when
                );
            }
            default -> throw new IllegalArgumentException("Unsupported appointment event type: " + eventType);
        }

        Notification notification = Notification.builder()
                .recipientUsername(patient)
                .title(title)
                .message(message)
                .type(type)
                .relatedAppointmentId(event.getAppointmentId())
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Very simple time interval formatting.
     * Example: "2025-12-06T18:30 - 19:00"
     */
    private String formatInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "an unspecified time";
        }
        return start.toString() + " - " + end.toLocalTime();
    }


}

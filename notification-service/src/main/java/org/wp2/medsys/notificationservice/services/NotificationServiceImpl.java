package org.wp2.medsys.notificationservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.domain.NotificationType;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;
import org.wp2.medsys.notificationservice.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;

    @Override
    @Transactional
    public Notification create(NotificationCreateDTO dto) {
        NotificationType type = mapType(dto.type());

        Notification notification = Notification.builder()
                .recipientUsername(dto.recipientUsername())
                .title(dto.title())
                .message(dto.message())
                .type(type)
                .relatedAppointmentId(dto.relatedAppointmentId())
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();

        return repo.save(notification);
    }

    @Override
    public List<Notification> findAll() {
        return repo.findAll();
    }

    @Override
    public List<Notification> findForRecipient(String username) {
        return repo.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public List<Notification> findForRecipient(String username, NotificationStatus status) {
        return repo.findByRecipientUsernameAndStatusOrderByCreatedAtDesc(username, status);
    }

    @Override
    public long countUnread(String username) {
        return repo.countByRecipientUsernameAndStatus(username, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long id, String username) {
        Notification n = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));

        if (!n.getRecipientUsername().equals(username)) {
            throw new AccessDeniedException("You can only read your own notifications.");
        }

        if (n.getStatus() == NotificationStatus.UNREAD) {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
        }

        return n;
    }

    @Override
    @Transactional
    public int markAllAsRead(String username) {
        List<Notification> unread = repo.findByRecipientUsernameAndStatusOrderByCreatedAtDesc(
                username,
                NotificationStatus.UNREAD
        );

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(now);
        });

        repo.saveAll(unread);
        return unread.size();
    }

    /* ---------- helpers ---------- */

    private NotificationType mapType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return NotificationType.APPOINTMENT_CREATED;
        }
        String normalized = rawType.trim().toUpperCase();
        return switch (normalized) {
            case "APPOINTMENT_CREATED" -> NotificationType.APPOINTMENT_CREATED;
            case "APPOINTMENT_ACCEPTED" -> NotificationType.APPOINTMENT_ACCEPTED;
            case "APPOINTMENT_DENIED" -> NotificationType.APPOINTMENT_DENIED;
            case "APPOINTMENT_CANCELLED" -> NotificationType.APPOINTMENT_CANCELLED;
            default -> NotificationType.APPOINTMENT_CREATED;
        };
    }
}

package org.wp2.medsys.notificationservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    // Notifications for a user with a given status, newest first
    List<Notification> findByRecipientUsernameAndStatusOrderByCreatedAtDesc(
            String recipientUsername,
            NotificationStatus status
    );

    // Count unread for a user
    long countByRecipientUsernameAndStatus(String recipientUsername, NotificationStatus status);

    // For markAsRead: find one notification that belongs to a given user
    Optional<Notification> findByIdAndRecipientUsername(Long id, String recipientUsername);

    // For markAllAsRead: get all unread without ordering requirement
    List<Notification> findByRecipientUsernameAndStatus(
            String recipientUsername,
            NotificationStatus status
    );
}

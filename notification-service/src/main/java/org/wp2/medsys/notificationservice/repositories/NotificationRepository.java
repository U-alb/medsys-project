package org.wp2.medsys.notificationservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    List<Notification> findByRecipientUsernameAndStatusOrderByCreatedAtDesc(
            String recipientUsername,
            NotificationStatus status
    );

    long countByRecipientUsernameAndStatus(String recipientUsername, NotificationStatus status);
}

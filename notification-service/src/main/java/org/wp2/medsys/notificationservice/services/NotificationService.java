package org.wp2.medsys.notificationservice.services;

import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;

import java.util.List;

public interface NotificationService {

    Notification create(NotificationCreateDTO dto);

    List<Notification> findAll();

    List<Notification> findForRecipient(String username);

    List<Notification> findForRecipient(String username, NotificationStatus status);

    long countUnread(String username);

    Notification markAsRead(Long id, String username);

    int markAllAsRead(String username);
}

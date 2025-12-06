package org.wp2.medsys.notificationservice.services;

import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;
import org.wp2.medsys.notificationservice.messaging.AppointmentEvent;

import java.util.List;

/**
 * Application-layer API for working with notifications.
 */
public interface NotificationService {

    /**
     * Create and persist a new notification based on the DTO.
     */
    Notification create(NotificationCreateDTO dto);

    /**
     * List all notifications in the system (debug/admin use).
     */
    List<Notification> findAll();

    /**
     * List all notifications for a given user (most recent first).
     */
    List<Notification> findForRecipient(String username);

    /**
     * List notifications for a given user filtered by status.
     */
    List<Notification> findForRecipient(String username, NotificationStatus status);

    /**
     * Count how many unread notifications a user has.
     */
    long countUnread(String username);

    /**
     * Mark a single notification as read for a given user.
     */
    Notification markAsRead(Long id, String username);

    /**
     * Mark all notifications as read for a given user.
     *
     * @return how many were updated
     */
    int markAllAsRead(String username);

    /**
     * Convenience method for internal callers that just know
     * "username + message". It will derive a reasonable title and type.
     */
    Notification createNotification(String username, String message);

    /**
     * Create and persist a notification derived from an appointment lifecycle event.
     */
    Notification createFromAppointmentEvent(AppointmentEvent event);

}

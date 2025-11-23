package org.wp2.medsys.notificationservice.dto;

import lombok.Data;

/**
 * Simple payload used by other services (or Postman)
 * to create a notification for a user.
 */
@Data
public class CreateNotificationRequest {

    private String username;  // who will see the notification
    private String message;   // text that will be displayed
}

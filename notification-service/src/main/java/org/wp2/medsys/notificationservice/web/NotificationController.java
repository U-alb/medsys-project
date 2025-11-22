package org.wp2.medsys.notificationservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.dto.NotificationResponse;
import org.wp2.medsys.notificationservice.services.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientUsername(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getRelatedAppointmentId(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }

    /**
     * Current user notifications (optionally filtered by status).
     * GET /notifications/mine?status=UNREAD
     */
    @GetMapping("/mine")
    public ResponseEntity<List<NotificationResponse>> mine(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        String username = authentication.getName();

        NotificationStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            try {
                filterStatus = NotificationStatus.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status filter: " + normalized);
            }
        }

        List<Notification> list = (filterStatus == null)
                ? notificationService.findForRecipient(username)
                : notificationService.findForRecipient(username, filterStatus);

        List<NotificationResponse> response = list.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Unread count for the current user.
     * GET /notifications/unread-count
     */
    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(Authentication authentication) {
        String username = authentication.getName();
        long count = notificationService.countUnread(username);
        return Map.of(
                "username", username,
                "unreadCount", count
        );
    }

    /**
     * Mark a single notification as read (if it belongs to current user).
     * POST /notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        Notification updated = notificationService.markAsRead(id, username);
        return toResponse(updated);
    }

    /**
     * Mark all notifications for current user as read.
     * POST /notifications/read-all
     */
    @PostMapping("/read-all")
    public Map<String, Object> markAllRead(Authentication authentication) {
        String username = authentication.getName();
        int updated = notificationService.markAllAsRead(username);
        return Map.of(
                "username", username,
                "updated", updated
        );
    }
}

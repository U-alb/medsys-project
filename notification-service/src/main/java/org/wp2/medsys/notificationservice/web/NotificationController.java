package org.wp2.medsys.notificationservice.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;
import org.wp2.medsys.notificationservice.dto.NotificationResponseDTO;
import org.wp2.medsys.notificationservice.services.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /* -------- internal endpoint (called by appointments-service) -------- */

    @PostMapping("/internal/create")
    public ResponseEntity<NotificationResponseDTO> createInternal(
            @Valid @RequestBody NotificationCreateDTO dto
    ) {
        Notification notification = notificationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(notification));
    }

    /* -------- admin: see all notifications -------- */

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificationResponseDTO> getAll() {
        return notificationService.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /* -------- current user: notifications -------- */

    @GetMapping("/mine")
    public List<NotificationResponseDTO> getMine(
            Authentication authentication,
            @RequestParam(name = "status", required = false) NotificationStatus status
    ) {
        String username = (String) authentication.getPrincipal();

        List<Notification> notifications;
        if (status == null) {
            notifications = notificationService.findForRecipient(username);
        } else {
            notifications = notificationService.findForRecipient(username, status);
        }

        return notifications.stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/mine/unread-count")
    public Map<String, Long> countUnreadMine(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        long count = notificationService.countUnread(username);
        return Map.of("unreadCount", count);
    }

    @PostMapping("/{id}/mark-read")
    public NotificationResponseDTO markRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = (String) authentication.getPrincipal();
        Notification updated = notificationService.markAsRead(id, username);
        return toDto(updated);
    }

    @PostMapping("/mark-all-read")
    public Map<String, Integer> markAllRead(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        int updatedCount = notificationService.markAllAsRead(username);
        return Map.of("updated", updatedCount);
    }

    /* -------- mapping helper -------- */

    private NotificationResponseDTO toDto(Notification n) {
        return new NotificationResponseDTO(
                n.getId(),
                n.getRecipientUsername(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getStatus(),
                n.getRelatedAppointmentId(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}

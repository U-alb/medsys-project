package org.wp2.medsys.notificationservice.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.dto.NotificationCreateDTO;
import org.wp2.medsys.notificationservice.dto.NotificationResponse;
import org.wp2.medsys.notificationservice.services.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/debug/notifications")
@RequiredArgsConstructor
public class DebugNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationCreateDTO dto) {
        Notification created = notificationService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    @GetMapping
    public List<NotificationResponse> listAll() {
        return notificationService.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

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
}

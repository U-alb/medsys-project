package org.wp2.medsys.notificationservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wp2.medsys.notificationservice.domain.Notification;
import org.wp2.medsys.notificationservice.domain.NotificationStatus;
import org.wp2.medsys.notificationservice.repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void findForRecipient_returnsNotificationsForUser_whenStatusNull() {
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setRecipientUsername("patient1");
        n1.setStatus(NotificationStatus.UNREAD);
        n1.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("patient1"))
                .thenReturn(Arrays.asList(n1));

        List<Notification> list = notificationService.findForRecipient("patient1", null);

        assertEquals(1, list.size());
        assertEquals("patient1", list.get(0).getRecipientUsername());
    }

    @Test
    void findForRecipient_filtersByStatus_whenProvided() {
        Notification n1 = new Notification();
        n1.setId(2L);
        n1.setRecipientUsername("patient1");
        n1.setStatus(NotificationStatus.READ);
        n1.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findByRecipientUsernameAndStatusOrderByCreatedAtDesc("patient1", NotificationStatus.READ))
                .thenReturn(Arrays.asList(n1));

        List<Notification> list = notificationService.findForRecipient("patient1", NotificationStatus.READ);

        assertEquals(1, list.size());
        assertEquals(NotificationStatus.READ, list.get(0).getStatus());
    }

    @Test
    void countUnread_returnsCorrectNumber() {
        when(notificationRepository.countByRecipientUsernameAndStatus("patient1", NotificationStatus.UNREAD)).thenReturn(3L);

        long count = notificationService.countUnread("patient1");

        assertEquals(3L, count);
    }

    @Test
    void markAsRead_marksAndReturnsNotification() {
        Notification n = new Notification();
        n.setId(2L);
        n.setRecipientUsername("patient1");
        n.setStatus(NotificationStatus.UNREAD);

        when(notificationRepository.findByIdAndRecipientUsername(2L, "patient1"))
                .thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification updated = notificationService.markAsRead(2L, "patient1");

        assertEquals(NotificationStatus.READ, updated.getStatus());
        assertNotNull(updated.getReadAt());
    }

    @Test
    void markAsRead_notFound_throws() {
        when(notificationRepository.findByIdAndRecipientUsername(999L, "patient1"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificationService.markAsRead(999L, "patient1"));
    }

    @Test
    void markAllRead_updatesAllUnreadAndReturnsCount() {
        Notification n1 = new Notification();
        n1.setId(3L);
        n1.setRecipientUsername("patient1");
        n1.setStatus(NotificationStatus.UNREAD);

        Notification n2 = new Notification();
        n2.setId(4L);
        n2.setRecipientUsername("patient1");
        n2.setStatus(NotificationStatus.UNREAD);

        when(notificationRepository.findByRecipientUsernameAndStatus("patient1", NotificationStatus.UNREAD))
                .thenReturn(Arrays.asList(n1, n2));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int updated = notificationService.markAllAsRead("patient1");

        assertEquals(2, updated);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}

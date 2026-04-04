package com.example.project.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.User;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.notification.model.Notification;
import com.example.project.notification.model.NotificationType;
import com.example.project.notification.observer.NotificationSubject;
import com.example.project.notification.repository.NotificationRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Given exchange request and actor when publishExchangeRequestSent then exchange event is published")
    void givenExchangeRequestAndActor_whenPublishExchangeRequestSent_thenExchangeEventIsPublished() {
        ExchangeRequest exchangeRequest = new ExchangeRequest();
        exchangeRequest.setId(100L);

        notificationService.publishExchangeRequestSent(exchangeRequest, 11L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationSubject).publish(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertEquals(NotificationEventType.EXCHANGE_REQUEST_SENT, event.eventType());
        assertEquals(11L, event.actorUserId());
        assertEquals(exchangeRequest, event.exchangeRequest());
        assertNull(event.deliveryOffer());
        assertNull(event.directRecipient());
        assertNull(event.customMessage());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Given delivery offer and actor when publishDeliveryAssigned then delivery event is published")
    void givenDeliveryOfferAndActor_whenPublishDeliveryAssigned_thenDeliveryEventIsPublished() {
        DeliveryOffer deliveryOffer = new DeliveryOffer();
        deliveryOffer.setId(200L);

        notificationService.publishDeliveryAssigned(deliveryOffer, 17L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationSubject).publish(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertEquals(NotificationEventType.DELIVERY_ASSIGNED, event.eventType());
        assertEquals(17L, event.actorUserId());
        assertEquals(deliveryOffer, event.deliveryOffer());
        assertNull(event.exchangeRequest());
        assertNull(event.directRecipient());
        assertNull(event.customMessage());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Given delivery offer and actor when publishFirstPickupApproaching then stage event is published")
    void givenDeliveryOfferAndActor_whenPublishFirstPickupApproaching_thenStageEventIsPublished() {
        DeliveryOffer deliveryOffer = new DeliveryOffer();
        deliveryOffer.setId(201L);

        notificationService.publishFirstPickupApproaching(deliveryOffer, 18L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationSubject).publish(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertEquals(NotificationEventType.FIRST_PICKUP_APPROACHING, event.eventType());
        assertEquals(18L, event.actorUserId());
        assertEquals(deliveryOffer, event.deliveryOffer());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Given null recipient and empty message when publishSystemNotification then system event is published")
    void givenNullRecipientAndEmptyMessage_whenPublishSystemNotification_thenSystemEventIsPublished() {
        notificationService.publishSystemNotification(null, "", 25L);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationSubject).publish(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertEquals(NotificationEventType.SYSTEM_ANNOUNCEMENT, event.eventType());
        assertEquals(25L, event.actorUserId());
        assertNull(event.directRecipient());
        assertEquals("", event.customMessage());
        assertNull(event.exchangeRequest());
        assertNull(event.deliveryOffer());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Given authenticated user and unread filter when getCurrentUserNotifications then unread notifications are returned")
    void givenAuthenticatedUserAndUnreadFilter_whenGetCurrentUserNotifications_thenUnreadNotificationsAreReturned() {
        User user = user(1L, "reader@example.com");
        Notification first = notification(10L, user, "Unread delivery update", NotificationType.DELIVERY, false,
            LocalDateTime.of(2026, 4, 1, 9, 0));
        Notification second = notification(11L, user, "Unread exchange update", NotificationType.EXCHANGE, false,
            LocalDateTime.of(2026, 4, 1, 8, 0));

        when(securityUtil.getCurrentUsername()).thenReturn("reader@example.com");
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
            .thenReturn(List.of(first, second));

        List<NotificationView> result = notificationService.getCurrentUserNotifications(true, 5);

        assertEquals(2, result.size());
        assertEquals("Unread delivery update", result.get(0).message());
        assertEquals("DELIVERY", result.get(0).type());
        assertFalse(result.get(0).read());
        assertNotNull(result.get(0).timestamp());
        assertNotNull(result.get(0).timestampEpochMillis());
        assertEquals("Asia/Dhaka", result.get(0).timezone());
        verify(notificationRepository).findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(
            eq(1L),
            argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 5)
        );
        verify(notificationRepository, never()).findByRecipient_IdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Given anonymous user when getCurrentUserNotifications then empty list is returned")
    void givenAnonymousUser_whenGetCurrentUserNotifications_thenEmptyListIsReturned() {
        when(securityUtil.getCurrentUsername()).thenReturn("anonymousUser");

        List<NotificationView> result = notificationService.getCurrentUserNotifications(false, 10);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("Given authenticated user when getCurrentUserSummary then unread count and recent notifications are returned")
    void givenAuthenticatedUser_whenGetCurrentUserSummary_thenUnreadCountAndRecentNotificationsAreReturned() {
        User user = user(7L, "summary@example.com");
        Notification notification = notification(31L, user, "Summary item", NotificationType.EXCHANGE, false,
            LocalDateTime.of(2026, 4, 1, 10, 30));

        when(securityUtil.getCurrentUsername()).thenReturn("summary@example.com");
        when(userRepository.findByEmail("summary@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.countByRecipient_IdAndIsReadFalse(7L)).thenReturn(3L);
        when(notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
            .thenReturn(List.of(notification));

        NotificationSummaryView summary = notificationService.getCurrentUserSummary(3);

        assertEquals(3L, summary.unreadCount());
        assertEquals(1, summary.recent().size());
        assertEquals("Summary item", summary.recent().get(0).message());
        verify(notificationRepository).countByRecipient_IdAndIsReadFalse(7L);
        verify(notificationRepository).findByRecipient_IdOrderByCreatedAtDesc(
            eq(7L),
            argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 3)
        );
    }

    @Test
    @DisplayName("Given owned unread notification when markAsRead then notification is updated")
    void givenOwnedUnreadNotification_whenMarkAsRead_thenNotificationIsUpdated() {
        User user = user(9L, "owner@example.com");
        Notification notification = notification(41L, user, "Mark me read", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 11, 0));

        when(securityUtil.getCurrentUsername()).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndRecipient_Id(41L, 9L)).thenReturn(Optional.of(notification));

        boolean updated = notificationService.markAsRead(41L);

        assertTrue(updated);
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Given notification not found when markAsRead then returns false")
    void givenNotificationNotFound_whenMarkAsRead_thenReturnsFalse() {
        User user = user(12L, "missing@example.com");

        when(securityUtil.getCurrentUsername()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndRecipient_Id(404L, 12L)).thenReturn(Optional.empty());

        boolean updated = notificationService.markAsRead(404L);

        assertFalse(updated);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Given authenticated user when markAllAsRead then updated count is returned")
    void givenAuthenticatedUser_whenMarkAllAsRead_thenUpdatedCountIsReturned() {
        User user = user(13L, "markall@example.com");

        when(securityUtil.getCurrentUsername()).thenReturn("markall@example.com");
        when(userRepository.findByEmail("markall@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.markAllReadByRecipientId(13L)).thenReturn(4);

        int updatedCount = notificationService.markAllAsRead();

        assertEquals(4, updatedCount);
        verify(notificationRepository).markAllReadByRecipientId(13L);
    }

    @Test
    @DisplayName("Given anonymous user when markAllAsRead then returns zero")
    void givenAnonymousUser_whenMarkAllAsRead_thenReturnsZero() {
        when(securityUtil.getCurrentUsername()).thenReturn("anonymousUser");

        int updatedCount = notificationService.markAllAsRead();

        assertEquals(0, updatedCount);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(notificationRepository);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        return user;
    }

    private Notification notification(
        Long id,
        User recipient,
        String message,
        NotificationType type,
        boolean isRead,
        LocalDateTime createdAt
    ) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(isRead);
        notification.setCreatedAt(createdAt);
        notification.setEventKey("event-" + id);
        return notification;
    }
}

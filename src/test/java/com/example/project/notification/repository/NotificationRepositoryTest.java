package com.example.project.notification.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.project.entity.User;
import com.example.project.notification.model.Notification;
import com.example.project.notification.model.NotificationType;
import com.example.project.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("NotificationRepository Data Layer Tests")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User recipientOne;
    private User recipientTwo;

    @BeforeEach
    void setUp() {
        recipientOne = saveUser("reader1@example.com", "Reader One");
        recipientTwo = saveUser("reader2@example.com", "Reader Two");
    }

    @Test
    @DisplayName("Given recipient notifications when finding with limit then most recent notifications are returned first")
    void givenRecipientNotifications_whenFindingWithLimit_thenMostRecentNotificationsAreReturnedFirst() {
        saveNotification(recipientOne, "older", NotificationType.EXCHANGE, false,
            LocalDateTime.of(2026, 4, 1, 8, 0), "event-old");
        saveNotification(recipientOne, "newer", NotificationType.DELIVERY, false,
            LocalDateTime.of(2026, 4, 1, 9, 0), "event-new");
        saveNotification(recipientOne, "newest", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 10, 0), "event-newest");
        saveNotification(recipientTwo, "other user", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 11, 0), "event-other");

        List<Notification> result = notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(
            recipientOne.getId(),
            PageRequest.of(0, 2)
        );

        assertEquals(2, result.size());
        assertEquals("newest", result.get(0).getMessage());
        assertEquals("newer", result.get(1).getMessage());
        assertTrue(result.stream().allMatch(n -> n.getRecipient().getId().equals(recipientOne.getId())));
    }

    @Test
    @DisplayName("Given mixed read states when finding unread then only unread notifications are returned")
    void givenMixedReadStates_whenFindingUnread_thenOnlyUnreadNotificationsAreReturned() {
        saveNotification(recipientOne, "unread one", NotificationType.EXCHANGE, false,
            LocalDateTime.of(2026, 4, 1, 9, 0), "event-unread-one");
        saveNotification(recipientOne, "already read", NotificationType.EXCHANGE, true,
            LocalDateTime.of(2026, 4, 1, 10, 0), "event-read");
        saveNotification(recipientOne, "unread two", NotificationType.DELIVERY, false,
            LocalDateTime.of(2026, 4, 1, 11, 0), "event-unread-two");

        List<Notification> unread = notificationRepository.findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(
            recipientOne.getId(),
            PageRequest.of(0, 10)
        );

        assertEquals(2, unread.size());
        assertTrue(unread.stream().noneMatch(Notification::isRead));
        assertEquals("unread two", unread.get(0).getMessage());
    }

    @Test
    @DisplayName("Given unread and read notifications when counting unread then unread count is returned")
    void givenUnreadAndReadNotifications_whenCountingUnread_thenUnreadCountIsReturned() {
        saveNotification(recipientOne, "unread one", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 9, 0), "count-unread-one");
        saveNotification(recipientOne, "unread two", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 10, 0), "count-unread-two");
        saveNotification(recipientOne, "read one", NotificationType.SYSTEM, true,
            LocalDateTime.of(2026, 4, 1, 11, 0), "count-read-one");

        long unreadCount = notificationRepository.countByRecipient_IdAndIsReadFalse(recipientOne.getId());

        assertEquals(2L, unreadCount);
    }

    @Test
    @DisplayName("Given existing notification and owner when finding by id and recipient then notification is returned")
    void givenExistingNotificationAndOwner_whenFindingByIdAndRecipient_thenNotificationIsReturned() {
        Notification saved = saveNotification(recipientOne, "find me", NotificationType.DELIVERY, false,
            LocalDateTime.of(2026, 4, 1, 12, 0), "find-owner");

        Optional<Notification> found = notificationRepository.findByIdAndRecipient_Id(saved.getId(), recipientOne.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Given notification owned by another recipient when finding by id and recipient then empty optional is returned")
    void givenNotificationOwnedByAnotherRecipient_whenFindingByIdAndRecipient_thenEmptyOptionalIsReturned() {
        Notification saved = saveNotification(recipientOne, "private notification", NotificationType.DELIVERY, false,
            LocalDateTime.of(2026, 4, 1, 12, 30), "find-other");

        Optional<Notification> found = notificationRepository.findByIdAndRecipient_Id(saved.getId(), recipientTwo.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Given event key and recipient when checking existence then result matches recipient and key")
    void givenEventKeyAndRecipient_whenCheckingExistence_thenResultMatchesRecipientAndKey() {
        saveNotification(recipientOne, "event key notification", NotificationType.EXCHANGE, false,
            LocalDateTime.of(2026, 4, 1, 13, 0), "event-exists");

        boolean existsForOwner = notificationRepository.existsByEventKeyAndRecipient_Id("event-exists", recipientOne.getId());
        boolean existsForOtherRecipient = notificationRepository.existsByEventKeyAndRecipient_Id("event-exists", recipientTwo.getId());
        boolean existsForMissingKey = notificationRepository.existsByEventKeyAndRecipient_Id("missing-key", recipientOne.getId());

        assertTrue(existsForOwner);
        assertFalse(existsForOtherRecipient);
        assertFalse(existsForMissingKey);
    }

    @Test
    @DisplayName("Given unread notifications when marking all as read then only target recipient notifications are updated")
    void givenUnreadNotifications_whenMarkingAllAsRead_thenOnlyTargetRecipientNotificationsAreUpdated() {
        saveNotification(recipientOne, "target unread one", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 14, 0), "mark-target-one");
        saveNotification(recipientOne, "target unread two", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 14, 1), "mark-target-two");
        saveNotification(recipientOne, "target already read", NotificationType.SYSTEM, true,
            LocalDateTime.of(2026, 4, 1, 14, 2), "mark-target-read");
        saveNotification(recipientTwo, "other user unread", NotificationType.SYSTEM, false,
            LocalDateTime.of(2026, 4, 1, 14, 3), "mark-other-one");

        int updatedCount = notificationRepository.markAllReadByRecipientId(recipientOne.getId());
        entityManager.flush();
        entityManager.clear();

        long unreadForTarget = notificationRepository.countByRecipient_IdAndIsReadFalse(recipientOne.getId());
        long unreadForOther = notificationRepository.countByRecipient_IdAndIsReadFalse(recipientTwo.getId());

        assertEquals(2, updatedCount);
        assertEquals(0L, unreadForTarget);
        assertEquals(1L, unreadForOther);
    }

    private User saveUser(String email, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("password");
        return userRepository.save(user);
    }

    private Notification saveNotification(
        User recipient,
        String message,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt,
        String eventKey
    ) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(read);
        notification.setCreatedAt(createdAt);
        notification.setEventKey(eventKey);
        Notification saved = notificationRepository.save(notification);
        entityManager.flush();
        return saved;
    }
}

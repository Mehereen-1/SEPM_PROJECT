package com.example.project.notification.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.User;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.notification.model.Notification;
import com.example.project.notification.observer.NotificationSubject;
import com.example.project.notification.repository.NotificationRepository;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final ZoneId NOTIFICATION_ZONE = ZoneId.of("Asia/Dhaka");

    private final NotificationSubject notificationSubject;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    public NotificationServiceImpl(
        NotificationSubject notificationSubject,
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        SecurityUtil securityUtil
    ) {
        this.notificationSubject = notificationSubject;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.securityUtil = securityUtil;
    }

    @Override
    public void publishExchangeRequestSent(ExchangeRequest exchangeRequest, Long actorUserId) {
        publish(NotificationEventType.EXCHANGE_REQUEST_SENT, actorUserId, exchangeRequest, null, null, null);
    }

    @Override
    public void publishExchangeAccepted(ExchangeRequest exchangeRequest, Long actorUserId) {
        publish(NotificationEventType.EXCHANGE_ACCEPTED, actorUserId, exchangeRequest, null, null, null);
    }

    @Override
    public void publishExchangeRejected(ExchangeRequest exchangeRequest, Long actorUserId) {
        publish(NotificationEventType.EXCHANGE_REJECTED, actorUserId, exchangeRequest, null, null, null);
    }

    @Override
    public void publishDeliveryCreated(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.DELIVERY_CREATED, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishDeliveryAssigned(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.DELIVERY_ASSIGNED, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishFirstPickupApproaching(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.FIRST_PICKUP_APPROACHING, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishPickupStarted(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.PICKUP_STARTED, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishSecondPickupApproaching(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.SECOND_PICKUP_APPROACHING, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishBookPicked(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.BOOK_PICKED, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishFinalDeliveryApproaching(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.FINAL_DELIVERY_APPROACHING, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishDeliveryCompleted(DeliveryOffer deliveryOffer, Long actorUserId) {
        publish(NotificationEventType.DELIVERY_COMPLETED, actorUserId, null, deliveryOffer, null, null);
    }

    @Override
    public void publishSystemNotification(User recipient, String message, Long actorUserId) {
        publish(NotificationEventType.SYSTEM_ANNOUNCEMENT, actorUserId, null, null, recipient, message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationView> getCurrentUserNotifications(Boolean unread, Integer limit) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, safeLimit);

        List<Notification> notifications = Boolean.TRUE.equals(unread)
            ? notificationRepository.findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(currentUser.get().getId(), pageable)
            : notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(currentUser.get().getId(), pageable);

        return notifications.stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryView getCurrentUserSummary(Integer recentLimit) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return new NotificationSummaryView(0, List.of());
        }

        long unreadCount = notificationRepository.countByRecipient_IdAndIsReadFalse(currentUser.get().getId());
        List<NotificationView> recent = getCurrentUserNotifications(false, recentLimit);
        return new NotificationSummaryView(unreadCount, recent);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty() || notificationId == null) {
            return false;
        }

        Optional<Notification> notificationResult = notificationRepository.findByIdAndRecipient_Id(notificationId, currentUser.get().getId());
        if (notificationResult.isEmpty()) {
            return false;
        }

        Notification notification = notificationResult.get();
        if (notification.isRead()) {
            return true;
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return true;
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return 0;
        }
        return notificationRepository.markAllReadByRecipientId(currentUser.get().getId());
    }

    private void publish(
        NotificationEventType eventType,
        Long actorUserId,
        ExchangeRequest exchangeRequest,
        DeliveryOffer deliveryOffer,
        User directRecipient,
        String customMessage
    ) {
        NotificationEvent event = new NotificationEvent(
            eventType,
            actorUserId,
            exchangeRequest,
            deliveryOffer,
            directRecipient,
            customMessage,
            LocalDateTime.now(NOTIFICATION_ZONE)
        );
        notificationSubject.publish(event);
    }

    private Optional<User> getCurrentUser() {
        String username = securityUtil.getCurrentUsername();
        if (username == null || "anonymousUser".equals(username)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(username);
    }

    private NotificationView toView(Notification notification) {
        OffsetDateTime timestamp = notification.getCreatedAt() != null
            ? notification.getCreatedAt().atZone(NOTIFICATION_ZONE).toOffsetDateTime()
            : null;
        Long timestampEpochMillis = timestamp != null ? timestamp.toInstant().toEpochMilli() : null;

        return new NotificationView(
            notification.getId(),
            notification.getMessage(),
            notification.getType().name(),
            notification.isRead(),
            timestamp,
            timestampEpochMillis,
            NOTIFICATION_ZONE.getId()
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }

        if (limit < 1) {
            return 1;
        }

        return Math.min(limit, 100);
    }
}

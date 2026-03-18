package com.example.project.notification.service;

import java.util.List;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.User;

public interface NotificationService {

    void publishExchangeRequestSent(ExchangeRequest exchangeRequest, Long actorUserId);

    void publishExchangeAccepted(ExchangeRequest exchangeRequest, Long actorUserId);

    void publishExchangeRejected(ExchangeRequest exchangeRequest, Long actorUserId);

    void publishDeliveryCreated(DeliveryOffer deliveryOffer, Long actorUserId);

    void publishDeliveryAssigned(DeliveryOffer deliveryOffer, Long actorUserId);

    void publishPickupStarted(DeliveryOffer deliveryOffer, Long actorUserId);

    void publishBookPicked(DeliveryOffer deliveryOffer, Long actorUserId);

    void publishDeliveryCompleted(DeliveryOffer deliveryOffer, Long actorUserId);

    void publishSystemNotification(User recipient, String message, Long actorUserId);

    List<NotificationView> getCurrentUserNotifications(Boolean unread, Integer limit);

    NotificationSummaryView getCurrentUserSummary(Integer recentLimit);

    boolean markAsRead(Long notificationId);

    int markAllAsRead();
}

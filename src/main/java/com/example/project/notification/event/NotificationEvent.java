package com.example.project.notification.event;

import java.time.LocalDateTime;

import com.example.project.entity.DeliveryOffer;
import com.example.project.entity.ExchangeRequest;
import com.example.project.entity.User;

public record NotificationEvent(
    NotificationEventType eventType,
    Long actorUserId,
    ExchangeRequest exchangeRequest,
    DeliveryOffer deliveryOffer,
    User directRecipient,
    String customMessage,
    LocalDateTime occurredAt
) {
}

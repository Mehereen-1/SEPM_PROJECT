package com.example.project.notification.strategy;

import com.example.project.entity.User;
import com.example.project.notification.model.NotificationType;

public record NotificationDraft(
    User recipient,
    String message,
    NotificationType type,
    String eventKey
) {
}

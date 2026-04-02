package com.example.project.notification.service;

import java.time.LocalDateTime;

public record NotificationView(
    Long id,
    String message,
    String type,
    boolean read,
    LocalDateTime timestamp
) {
}

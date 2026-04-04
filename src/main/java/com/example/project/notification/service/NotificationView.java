package com.example.project.notification.service;

import java.time.OffsetDateTime;

public record NotificationView(
    Long id,
    String message,
    String type,
    boolean read,
    OffsetDateTime timestamp,
    Long timestampEpochMillis,
    String timezone
) {
}

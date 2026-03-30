package com.example.project.notification.service;

import java.util.List;

public record NotificationSummaryView(
    long unreadCount,
    List<NotificationView> recent
) {
}

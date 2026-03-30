package com.example.project.notification.strategy;

import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;

import java.util.List;

public interface NotificationStrategy {
    boolean supports(NotificationEventType eventType);

    List<NotificationDraft> build(NotificationEvent event);
}

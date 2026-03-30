package com.example.project.notification.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.project.entity.User;
import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.event.NotificationEventType;
import com.example.project.notification.model.NotificationType;

@Component
public class SystemNotificationStrategy implements NotificationStrategy {

    @Override
    public boolean supports(NotificationEventType eventType) {
        return eventType == NotificationEventType.SYSTEM_ANNOUNCEMENT;
    }

    @Override
    public List<NotificationDraft> build(NotificationEvent event) {
        User recipient = event.directRecipient();
        if (recipient == null || event.customMessage() == null || event.customMessage().isBlank()) {
            return List.of();
        }

        String eventKey = "system:" + recipient.getId() + ":" + event.occurredAt();
        return List.of(new NotificationDraft(recipient, event.customMessage(), NotificationType.SYSTEM, eventKey));
    }
}

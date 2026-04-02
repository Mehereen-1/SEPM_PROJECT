package com.example.project.notification.observer;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.notification.event.NotificationEvent;
import com.example.project.notification.model.Notification;
import com.example.project.notification.repository.NotificationRepository;
import com.example.project.notification.strategy.NotificationDraft;
import com.example.project.notification.strategy.NotificationStrategy;
import com.example.project.notification.strategy.NotificationStrategyRegistry;

@Component
public class PersistingNotificationObserver implements NotificationObserver {

    private final NotificationStrategyRegistry strategyRegistry;
    private final NotificationRepository notificationRepository;

    public PersistingNotificationObserver(
        NotificationStrategyRegistry strategyRegistry,
        NotificationRepository notificationRepository
    ) {
        this.strategyRegistry = strategyRegistry;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void onEvent(NotificationEvent event) {
        NotificationStrategy strategy = strategyRegistry.resolve(event.eventType());
        List<NotificationDraft> drafts = strategy.build(event);

        Set<Long> recipientDedup = new HashSet<>();
        for (NotificationDraft draft : drafts) {
            if (!isDraftValid(draft)) {
                continue;
            }

            Long recipientId = draft.recipient().getId();
            if (event.actorUserId() != null && event.actorUserId().equals(recipientId)) {
                continue;
            }

            if (!recipientDedup.add(recipientId)) {
                continue;
            }

            if (draft.eventKey() != null && notificationRepository.existsByEventKeyAndRecipient_Id(draft.eventKey(), recipientId)) {
                continue;
            }

            Notification notification = new Notification();
            notification.setRecipient(draft.recipient());
            notification.setMessage(draft.message().trim());
            notification.setType(draft.type());
            notification.setRead(false);
            notification.setEventKey(draft.eventKey());
            notification.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private boolean isDraftValid(NotificationDraft draft) {
        return draft != null
            && draft.recipient() != null
            && draft.recipient().getId() != null
            && draft.message() != null
            && !draft.message().isBlank()
            && draft.type() != null;
    }
}

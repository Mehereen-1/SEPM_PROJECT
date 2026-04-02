package com.example.project.notification.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.project.notification.event.NotificationEventType;

@Component
public class NotificationStrategyRegistry {

    private final List<NotificationStrategy> strategies;

    public NotificationStrategyRegistry(List<NotificationStrategy> strategies) {
        this.strategies = strategies;
    }

    public NotificationStrategy resolve(NotificationEventType eventType) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(eventType))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No notification strategy registered for event " + eventType));
    }
}

package com.example.project.notification.observer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.project.notification.event.NotificationEvent;

@Component
public class DefaultNotificationSubject implements NotificationSubject {

    private final List<NotificationObserver> observers;

    public DefaultNotificationSubject(List<NotificationObserver> observers) {
        this.observers = observers;
    }

    @Override
    public void publish(NotificationEvent event) {
        for (NotificationObserver observer : observers) {
            observer.onEvent(event);
        }
    }
}

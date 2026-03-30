package com.example.project.notification.observer;

import com.example.project.notification.event.NotificationEvent;

public interface NotificationSubject {
    void publish(NotificationEvent event);
}

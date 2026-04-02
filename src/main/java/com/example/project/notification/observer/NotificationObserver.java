package com.example.project.notification.observer;

import com.example.project.notification.event.NotificationEvent;

public interface NotificationObserver {
    void onEvent(NotificationEvent event);
}

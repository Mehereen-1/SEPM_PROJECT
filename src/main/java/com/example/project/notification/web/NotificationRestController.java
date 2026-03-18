package com.example.project.notification.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.notification.service.NotificationService;
import com.example.project.notification.service.NotificationSummaryView;
import com.example.project.notification.service.NotificationView;

@RestController
@RequestMapping("/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationView>> listNotifications(
        @RequestParam(required = false) Boolean unread,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(notificationService.getCurrentUserNotifications(unread, limit));
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryView> getSummary(
        @RequestParam(required = false, defaultValue = "6") Integer limit
    ) {
        return ResponseEntity.ok(notificationService.getCurrentUserSummary(limit));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        boolean updated = notificationService.markAsRead(id);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @PatchMapping
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int updatedCount = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }
}

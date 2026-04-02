package com.example.project.notification.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationPageController {

    @GetMapping("/notification-center")
    public String notificationCenter() {
        return "notifications";
    }
}

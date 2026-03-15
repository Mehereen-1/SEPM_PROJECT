package com.example.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReaderDashboardController {

    @GetMapping("/reader/dashboard")
    public String readerDashboard(Model model) {
        // Placeholder for future dashboard data
        model.addAttribute("offers", java.util.Collections.emptyList());
        return "reader-dashboard";
    }
}

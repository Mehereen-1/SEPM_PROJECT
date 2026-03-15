package com.example.project.controller;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DeliveryDashboardController {

    @GetMapping("/delivery/dashboard")
    public String deliveryDashboard(Model model) {
        model.addAttribute("offers", Collections.emptyList());
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/offers")
    public String offers(Model model) {
        model.addAttribute("offers", Collections.emptyList());
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/pending")
    public String pending(Model model) {
        model.addAttribute("offers", Collections.emptyList());
        return "delivery-dashboard";
    }

    @GetMapping("/delivery/completed")
    public String completed(Model model) {
        model.addAttribute("offers", Collections.emptyList());
        return "delivery-dashboard";
    }
}

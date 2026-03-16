package com.example.project.controller;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReaderDashboardController {

    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/reader/dashboard")
    public String readerDashboard(Model model) {
        String currentEmail = securityUtil.getCurrentUsername();
        
        if (currentEmail != null) {
            User user = userRepository.findByEmail(currentEmail).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("userName", user.getName());
                model.addAttribute("userEmail", user.getEmail());
                
                // Sample activity data (in a real app, fetch from database)
                model.addAttribute("booksShared", 13);
                model.addAttribute("readersReached", 45);
            }
        }
        
        model.addAttribute("offers", java.util.Collections.emptyList());
        return "reader-dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String currentEmail = securityUtil.getCurrentUsername();
        
        if (currentEmail != null) {
            User user = userRepository.findByEmail(currentEmail).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("userName", user.getName());
                model.addAttribute("userEmail", user.getEmail());
            }
        }
        
        return "profile";
    }
}

package com.example.project.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;

@ControllerAdvice(annotations = Controller.class)
public class GlobalUserModelAttributes {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    public GlobalUserModelAttributes(UserRepository userRepository, SecurityUtil securityUtil) {
        this.userRepository = userRepository;
        this.securityUtil = securityUtil;
    }

    @ModelAttribute("currentUserFirstName")
    public String currentUserFirstName() {
        if (!securityUtil.isUserAuthenticated()) {
            return null;
        }

        String username = securityUtil.getCurrentUsername();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }

        Optional<User> userResult = userRepository.findByEmailIgnoreCase(username);
        if (userResult.isEmpty()) {
            return null;
        }

        User user = userResult.get();
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName().trim();
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            String[] parts = user.getName().trim().split("\\s+");
            return parts.length > 0 ? parts[0] : null;
        }

        return null;
    }
}
package com.example.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.project.service.UserService;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @GetMapping("/auth")
    public String auth(@RequestParam(value = "role", required = false, defaultValue = "BOOK_FRIEND") String role,
                       @RequestParam(value = "tab", required = false, defaultValue = "signUp") String tab,
                       Model model) {
        model.addAttribute("role", role);
        model.addAttribute("activeTab", tab);
        model.addAttribute("userForm", new com.example.project.entity.User());
        return "auth";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "role", required = false, defaultValue = "BOOK_FRIEND") String role,
                        Model model) {
        return "redirect:/auth?role=" + role + "&tab=signIn";
    }

    @GetMapping("/register")
    public String register(@RequestParam(value = "role", required = false, defaultValue = "BOOK_FRIEND") String role,
                           Model model) {
        return "redirect:/auth?role=" + role + "&tab=signUp";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute RegisterRequest request, Model model) {
        try {
            System.out.println(">>> REGISTRATION REQUEST RECEIVED");
            System.out.println("Form Data:");
            System.out.println("  - FirstName: '" + request.getFirstName() + "'");
            System.out.println("  - LastName: '" + request.getLastName() + "'");
            System.out.println("  - Email: '" + request.getEmail() + "'");
            System.out.println("  - Role: '" + request.getRole() + "'");
            
            String firstName = request.getFirstName() != null ? request.getFirstName().trim() : "";
            String lastName = request.getLastName() != null ? request.getLastName().trim() : "";
            String name = (firstName + " " + lastName).trim();
            String email = request.getEmail() != null ? request.getEmail().trim() : "";
            String password = request.getPassword();
            String role = request.getRole() != null && !request.getRole().isEmpty() ? request.getRole().trim() : "BOOK_FRIEND";
            
            System.out.println("Processed Role for UserService: '" + role + "'");
            
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (email.isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
            
            userService.registerUser(name, email, password, role);
            System.out.println(">>> REGISTRATION SUCCESS");
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            System.out.println(">>> REGISTRATION FAILED: " + e.getMessage());
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("role", request.getRole() != null ? request.getRole() : "BOOK_FRIEND");
            model.addAttribute("activeTab", "signUp");
            return "auth";
        }
    }
}

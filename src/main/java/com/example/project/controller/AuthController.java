package com.example.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.project.service.UserService;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "role", required = false, defaultValue = "user") String role,
                        Model model) {
        model.addAttribute("role", role);
        model.addAttribute("activeTab", "signIn");
        return "auth";
    }

    @GetMapping("/register")
    public String register(@RequestParam(value = "role", required = false, defaultValue = "user") String role,
                           Model model) {
        model.addAttribute("role", role);
        model.addAttribute("activeTab", "signUp");
        model.addAttribute("userForm", new com.example.project.entity.User());
        return "auth";
    }

    @GetMapping("/auth/{role}")
    public String authRole(@PathVariable("role") String role, Model model) {
        model.addAttribute("role", role);
        model.addAttribute("activeTab", "signUp");
        model.addAttribute("userForm", new com.example.project.entity.User());
        return "auth";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("userForm") com.example.project.entity.User user, Model model) {
        try {
            userService.registerUser(user.getName(), user.getEmail(), user.getPassword());
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("role", "user");
            model.addAttribute("activeTab", "signUp");
            return "auth";
        }
    }
}

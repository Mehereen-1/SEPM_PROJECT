package com.example.project.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String email, String password) {
        String defaultName = email != null ? email.split("@")[0] : "user";
        return registerUser(defaultName, email, password);
    }

    @Transactional
    public User registerUser(String name, String email, String password) {
        // Default to BOOK_FRIEND so existing calls still work.
        return registerUser(name, email, password, "BOOK_FRIEND");
    }

    @Transactional
    public User registerUser(String name, String email, String password, String roleName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already in use");
        }

        System.out.println("=== USER REGISTRATION ===");
        System.out.println("Email: " + email);
        System.out.println("Raw Role Input: '" + roleName + "'");
        
        String normalizedRole = roleName != null && !roleName.isEmpty() ? roleName.trim().toUpperCase() : "BOOK_FRIEND";
        System.out.println("Normalized Role: '" + normalizedRole + "'");
        
        // Ensure role exists in database
        Role userRole = roleRepository.findByName(normalizedRole)
                .orElseGet(() -> {
                    System.out.println("Role not found, creating new: " + normalizedRole);
                    Role r = new Role();
                    r.setName(normalizedRole);
                    return roleRepository.save(r);
                });

        System.out.println("Found/Created Role ID: " + userRole.getId() + ", Name: " + userRole.getName());

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Collections.singleton(userRole));
        
        User savedUser = userRepository.save(user);
        System.out.println("REGISTERED: " + savedUser.getEmail() + " with role ID: " + userRole.getId());
        System.out.println("========================");
        
        return savedUser;
    }
}


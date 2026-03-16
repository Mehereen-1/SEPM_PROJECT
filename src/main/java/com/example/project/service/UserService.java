package com.example.project.service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("Email already in use");
        }

        System.out.println("=== USER REGISTRATION ===");
        System.out.println("Email: " + email);
        System.out.println("Raw Role Input: '" + roleName + "'");
        
        String normalizedRole = roleName != null && !roleName.isEmpty() ? roleName.trim().toUpperCase() : "BOOK_FRIEND";
        System.out.println("Normalized Role: '" + normalizedRole + "'");
        
        // Ensure role exists in database
        Role userRole = resolveOrCreateRole(normalizedRole);

        System.out.println("Found/Created Role ID: " + userRole.getId() + ", Name: " + userRole.getName());

        User user = new User();
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Collections.singleton(userRole));
        
        User savedUser = userRepository.save(user);
        System.out.println("REGISTERED: " + savedUser.getEmail() + " with role ID: " + userRole.getId());
        System.out.println("========================");
        
        return savedUser;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Role resolveOrCreateRole(String normalizedRole) {
        Role directMatch = roleRepository.findByNameIgnoreCase(normalizedRole).orElse(null);
        if (directMatch != null) {
            return directMatch;
        }

        String roleWithoutPrefix = normalizedRole.startsWith("ROLE_")
                ? normalizedRole.substring("ROLE_".length())
                : normalizedRole;

        Role strippedMatch = roleRepository.findByNameIgnoreCase(roleWithoutPrefix).orElse(null);
        if (strippedMatch != null) {
            return strippedMatch;
        }

        boolean deliveryRequested = roleWithoutPrefix.contains("DELIVERY");
        List<String> aliases = deliveryRequested
                ? List.of("DELIVERY_PARTNER", "DELIVERY", "ROLE_DELIVERY_PARTNER", "ROLE_DELIVERY")
                : List.of("BOOK_FRIEND", "READER", "USER", "ROLE_BOOK_FRIEND", "ROLE_READER", "ROLE_USER");

        for (String alias : aliases) {
            Role existingAlias = roleRepository.findByNameIgnoreCase(alias).orElse(null);
            if (existingAlias != null) {
                return existingAlias;
            }
        }

        String roleToCreate = deliveryRequested ? "DELIVERY_PARTNER" : "BOOK_FRIEND";
        System.out.println("Role not found, creating new: " + roleToCreate);
        Role role = new Role();
        role.setName(roleToCreate);
        return roleRepository.save(role);
    }
}


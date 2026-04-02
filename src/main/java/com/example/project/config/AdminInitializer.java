package com.example.project.config;

import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Component
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeAdminUser() {
        String normalizedEmail = normalize(adminEmail);
        String rawPassword = adminPassword == null ? "" : adminPassword.trim();

        if (normalizedEmail.isEmpty() || rawPassword.isEmpty()) {
            log.warn("Admin initializer skipped: ADMIN_EMAIL or ADMIN_PASSWORD is missing.");
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Admin initializer: user already exists for {}. Skipping.", normalizedEmail);
            return;
        }

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    return roleRepository.save(role);
                });

        User admin = new User();
        admin.setName("System Administrator");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setActive(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);
        log.info("Admin initializer: default admin user created for {}.", normalizedEmail);
    }

    private String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
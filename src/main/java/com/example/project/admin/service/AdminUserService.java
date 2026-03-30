package com.example.project.admin.service;

import com.example.project.admin.dto.AdminUserResponse;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminUserService implements IAdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public ResponseEntity<?> blockUser(Long id) {
        Optional<User> targetUserResult = userRepository.findById(id);
        if (targetUserResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found."));
        }

        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername != null && !"anonymousUser".equals(currentUsername)) {
            Optional<User> currentUserResult = userRepository.findByEmailIgnoreCase(currentUsername);
            if (currentUserResult.isPresent() && currentUserResult.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Admin cannot block themselves."));
            }
        }

        User targetUser = targetUserResult.get();
        targetUser.setActive(false);
        User saved = userRepository.save(targetUser);

        return ResponseEntity.ok(toResponse(saved));
    }

    private AdminUserResponse toResponse(User user) {
        String role = user.getRoles().stream()
            .map(Role::getName)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.joining(","));

        String status = Boolean.FALSE.equals(user.getActive()) ? "DEACTIVATED" : "ACTIVE";

        return new AdminUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            role,
            status
        );
    }
}

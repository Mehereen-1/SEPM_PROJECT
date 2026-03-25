package com.example.project.admin.strategy;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class BlockUserStrategy implements AdminActionStrategy {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    public BlockUserStrategy(UserRepository userRepository, SecurityUtil securityUtil) {
        this.userRepository = userRepository;
        this.securityUtil = securityUtil;
    }

    @Override
    public void execute(Long id) {
        Optional<User> targetUserResult = userRepository.findById(id);
        if (targetUserResult.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "User not found.");
        }

        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername != null && !"anonymousUser".equals(currentUsername)) {
            Optional<User> currentUserResult = userRepository.findByEmailIgnoreCase(currentUsername);
            if (currentUserResult.isPresent() && currentUserResult.get().getId().equals(id)) {
                throw new ResponseStatusException(BAD_REQUEST, "Admin cannot block themselves.");
            }
        }

        User targetUser = targetUserResult.get();
        targetUser.setActive(false);
        userRepository.save(targetUser);
    }
}

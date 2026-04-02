package com.example.project.admin.service;

import com.example.project.admin.dto.AdminUserResponse;
import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("Given users exist when getAllUsers then mapped responses are returned")
    void givenUsersExist_whenGetAllUsers_thenMappedResponsesReturned() {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        Role userRole = new Role();
        userRole.setName("USER");

        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setName("Alice");
        activeUser.setEmail("alice@example.com");
        activeUser.setActive(true);
        activeUser.setRoles(Set.of(userRole, adminRole));

        User blockedUser = new User();
        blockedUser.setId(2L);
        blockedUser.setName("Bob");
        blockedUser.setEmail("bob@example.com");
        blockedUser.setActive(false);
        blockedUser.setRoles(Set.of(userRole));

        when(userRepository.findAll()).thenReturn(List.of(activeUser, blockedUser));

        List<AdminUserResponse> result = adminUserService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("ACTIVE", result.get(0).status());
        assertEquals("DEACTIVATED", result.get(1).status());
        assertEquals("ADMIN,USER", result.get(0).role());
    }

    @Test
    @DisplayName("Given missing target user when blockUser then not found returned")
    void givenMissingTargetUser_whenBlockUser_thenNotFound() {
        when(userRepository.findById(101L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminUserService.blockUser(101L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(java.util.Map.class, response.getBody());
    }

    @Test
    @DisplayName("Given admin tries to block self when blockUser then bad request returned")
    void givenAdminBlocksSelf_whenBlockUser_thenBadRequest() {
        User current = new User();
        current.setId(10L);
        current.setEmail("admin@example.com");

        User target = new User();
        target.setId(10L);
        target.setEmail("admin@example.com");
        target.setActive(true);

        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(securityUtil.getCurrentUsername()).thenReturn("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(current));

        ResponseEntity<?> response = adminUserService.blockUser(10L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userRepository, never()).save(target);
    }

    @Test
    @DisplayName("Given valid target user when blockUser then user is deactivated and returned")
    void givenValidTarget_whenBlockUser_thenUserDeactivated() {
        Role role = new Role();
        role.setName("USER");

        User target = new User();
        target.setId(77L);
        target.setName("Reader");
        target.setEmail("reader@example.com");
        target.setActive(true);
        target.setRoles(Set.of(role));

        when(userRepository.findById(77L)).thenReturn(Optional.of(target));
        when(securityUtil.getCurrentUsername()).thenReturn("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(target)).thenReturn(target);

        ResponseEntity<?> response = adminUserService.blockUser(77L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Boolean.FALSE.equals(target.getActive()));
        assertInstanceOf(AdminUserResponse.class, response.getBody());

        AdminUserResponse body = (AdminUserResponse) response.getBody();
        assertEquals("DEACTIVATED", body.status());
    }
}

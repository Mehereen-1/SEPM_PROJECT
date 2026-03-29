package com.example.project.service;

import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import com.example.project.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Given existing user with roles when loadUserByUsername then mapped authorities are returned")
    void givenExistingUserWithRoles_whenLoadUserByUsername_thenMappedAuthoritiesAreReturned() {
        Role role = new Role();
        role.setName("BOOK_FRIEND");

        User user = new User();
        user.setEmail("reader@example.com");
        user.setPassword("encoded-password");
        user.setRoles(Set.of(role));

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("reader@example.com");

        assertEquals("reader@example.com", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BOOK_FRIEND")));
    }

    @Test
    @DisplayName("Given unknown email when loadUserByUsername then username not found is thrown")
    void givenUnknownEmail_whenLoadUserByUsername_thenUsernameNotFoundIsThrown() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@example.com"));
    }

    @Test
    @DisplayName("Given user with no roles when loadUserByUsername then default book friend role is added")
    void givenUserWithNoRoles_whenLoadUserByUsername_thenDefaultBookFriendRoleIsAdded() {
        User user = new User();
        user.setEmail("norole@example.com");
        user.setPassword("encoded-password");
        user.setRoles(new HashSet<>());

        when(userRepository.findByEmailIgnoreCase("norole@example.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("norole@example.com");

        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BOOK_FRIEND")));
    }
}

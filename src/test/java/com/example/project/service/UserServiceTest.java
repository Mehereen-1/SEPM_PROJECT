package com.example.project.service;

import com.example.project.entity.Role;
import com.example.project.entity.User;
import com.example.project.repository.RoleRepository;
import com.example.project.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Given valid registration data when registerUser then user is saved with encoded password")
    void givenValidRegistrationData_whenRegisterUser_thenUserIsSavedWithEncodedPassword() {
        Role role = new Role();
        role.setId(1L);
        role.setName("BOOK_FRIEND");

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser("Alice", "Alice@Example.com", "secret123", "book_friend");

        assertEquals("alice@example.com", saved.getEmail());
        assertEquals("encoded-secret", saved.getPassword());
        assertEquals("Alice", saved.getName());
        assertTrue(saved.getRoles().contains(role));
        verify(passwordEncoder, times(1)).encode("secret123");
    }

    @Test
    @DisplayName("Given duplicate email when registerUser then exception is thrown")
    void givenDuplicateEmail_whenRegisterUser_thenExceptionIsThrown() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerUser("Alice", "alice@example.com", "secret123", "BOOK_FRIEND"));

        assertEquals("Email already in use", ex.getMessage());
    }

    @Test
    @DisplayName("Given blank email when registerUser then exception is thrown")
    void givenBlankEmail_whenRegisterUser_thenExceptionIsThrown() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerUser("Alice", "   ", "secret123", "BOOK_FRIEND"));

        assertEquals("Email is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given unknown reader role when registerUser then default role is created")
    void givenUnknownReaderRole_whenRegisterUser_thenDefaultRoleIsCreated() {
        when(userRepository.existsByEmailIgnoreCase("reader@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role created = invocation.getArgument(0);
            created.setId(10L);
            return created;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser("Reader", "reader@example.com", "password", "reader");

        Role assignedRole = saved.getRoles().iterator().next();
        assertEquals("BOOK_FRIEND", assignedRole.getName());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Given delivery role alias when registerUser then delivery partner role is resolved")
    void givenDeliveryRoleAlias_whenRegisterUser_thenDeliveryPartnerRoleIsResolved() {
        Role deliveryRole = new Role();
        deliveryRole.setId(2L);
        deliveryRole.setName("DELIVERY_PARTNER");

        when(userRepository.existsByEmailIgnoreCase("rider@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if ("DELIVERY_PARTNER".equalsIgnoreCase(arg)) {
                return Optional.of(deliveryRole);
            }
            return Optional.empty();
        });
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser("Rider", "rider@example.com", "password", "delivery");

        assertTrue(saved.getRoles().contains(deliveryRole));
    }

    @Test
    @DisplayName("Given full profile registration when registerUser then names and profile fields are persisted")
    void givenFullProfileRegistration_whenRegisterUser_thenNamesAndProfileFieldsArePersisted() {
        Role role = new Role();
        role.setId(1L);
        role.setName("BOOK_FRIEND");

        when(userRepository.existsByEmailIgnoreCase("mia@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser(
                "Mia",
                "Khan",
                "mia@example.com",
                "password",
                "BOOK_FRIEND",
                "Female",
                LocalDate.of(2000, 1, 1),
                "01700000000",
                "01800000000",
                23.70,
                90.40,
                "Dhaka"
        );

        assertEquals("Mia", saved.getFirstName());
        assertEquals("Khan", saved.getLastName());
        assertEquals("Mia Khan", saved.getName());
        assertEquals("Female", saved.getGender());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Given null user when updateProfile then illegal argument is thrown")
    void givenNullUser_whenUpdateProfile_thenIllegalArgumentIsThrown() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(null, "Alice", 23.0, 90.0, "Dhaka"));

        assertEquals("User is required", ex.getMessage());
    }

    @Test
    @DisplayName("Given user when updateProfile then mutable fields are trimmed and saved")
    void givenUser_whenUpdateProfile_thenMutableFieldsAreTrimmedAndSaved() {
        User user = new User();
        user.setName("Old Name");
        user.setAddress("Old Address");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.updateProfile(user, "  New Name  ", 24.1, 90.2, "  New Address  ");

        assertEquals("New Name", saved.getName());
        assertEquals("New Address", saved.getAddress());
        assertEquals(24.1, saved.getLatitude());
        assertEquals(90.2, saved.getLongitude());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Given role is resolved when registerUser then saved role instance is attached")
    void givenRoleIsResolved_whenRegisterUser_thenSavedRoleInstanceIsAttached() {
        Role role = new Role();
        role.setId(7L);
        role.setName("BOOK_FRIEND");

        when(userRepository.existsByEmailIgnoreCase("role@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("pw")).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser("Role User", "role@example.com", "pw", "BOOK_FRIEND");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertNotNull(captured.getRoles());
        assertEquals(1, captured.getRoles().size());
        assertSame(role, captured.getRoles().iterator().next());
    }
}

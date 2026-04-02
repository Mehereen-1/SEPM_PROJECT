# Authentication Testing Documentation

## 1. Overview

This project implements authentication using Spring Boot and Spring Security with the following core capabilities:

- User registration (`/api/auth/register` and `/register` form flow)
- User login (`/api/auth/login`)
- Password encoding through `PasswordEncoder` (BCrypt-based encoder configured in security config)
- Role-aware authorization and role resolution during registration

Authentication testing is critical because it validates security-sensitive paths that directly affect system trust and stability:

- Prevents invalid or duplicate account creation
- Confirms secure password handling behavior
- Verifies login pipeline behavior for valid and invalid credentials
- Ensures controller endpoints behave correctly under realistic request flows

---

## 2. Technologies Used

Authentication implementation and testing use the following technologies:

- Spring Boot 4.0.3
- Spring Security
- JUnit 5 (Jupiter)
- Mockito
- Spring Boot Test (`@SpringBootTest`)
- MockMvc (`@AutoConfigureMockMvc`)
- H2 in-memory database (test profile)
- Maven (Surefire test execution)

Relevant Maven test dependencies include:

- `spring-boot-starter-test`
- `spring-security-test`
- `spring-boot-webmvc-test`
- `com.h2database:h2`

---

## 3. Project Structure

Authentication-related production code:

- `src/main/java/com/example/project/controller/AuthController.java`
- `src/main/java/com/example/project/controller/AuthRestController.java`
- `src/main/java/com/example/project/service/UserService.java`
- `src/main/java/com/example/project/repository/UserRepository.java`
- `src/main/java/com/example/project/security/CustomUserDetailsService.java`
- `src/main/java/com/example/project/config/SecurityConfig.java`

Authentication-related tests:

- `src/test/java/com/example/project/service/UserServiceTest.java`
- `src/test/java/com/example/project/service/AuthServiceTest.java`
- `src/test/java/com/example/project/controller/AuthControllerIntegrationTest.java`

Test configuration:

- `src/test/resources/application-test.yaml`

Authentication test folder layout:

```text
src/
  test/
    java/
      com/example/project/
        service/
          UserServiceTest.java
          AuthServiceTest.java
        controller/
          AuthControllerIntegrationTest.java
    resources/
      application-test.yaml
```

---

## 4. Unit Testing (Service Layer)

### Scope

Unit tests focus on service-level authentication behavior and isolated business logic:

- `UserServiceTest`: registration, duplicate handling, password encoding, role assignment, profile updates
- `AuthServiceTest`: authentication user-details loading behavior (implemented through `CustomUserDetailsService`)

> Note: There is no standalone `AuthService` class in this codebase. The auth service-level behavior is covered by `UserService` and `CustomUserDetailsService`.

### Key Scenarios Covered

`UserServiceTest` validates:

- Successful user registration
- Duplicate email rejection
- Blank/missing email rejection
- Password encoding invocation
- Reader role fallback creation
- Delivery role alias resolution
- Profile-enriched registration flow
- Update profile behavior and validation

`AuthServiceTest` validates:

- Existing user with roles maps to expected Spring authorities
- Unknown user triggers `UsernameNotFoundException`
- Empty role set falls back to default `ROLE_BOOK_FRIEND`

### Mockito Usage

Mockito isolates services from external dependencies:

- `UserRepository`, `RoleRepository`, and `PasswordEncoder` are mocked
- Repository responses are stubbed with `when(...).thenReturn(...)`
- Save payloads are inspected using `ArgumentCaptor`
- Branch-specific logic is validated without database/network dependency

---

## 5. Integration Testing (Controller Layer)

### Scope

Integration tests validate request/response behavior for authentication endpoints using Spring context + MockMvc:

- Target controller: `AuthRestController` and form endpoint handling in `AuthController`
- Test class: `AuthControllerIntegrationTest`

### MockMvc-Based Scenarios

Covered integration scenarios:

- Register API success (`POST /api/auth/register`)
- Login success (`POST /api/auth/login`) and token response assertion
- Login failure for wrong credentials
- Validation error flow for missing registration form fields (`POST /register`)

### Why Integration Coverage Matters

These tests confirm that routing, binding, validation, controller logic, and security-adjacent behavior work together as expected in a real Spring test context.

---

## 6. Security Handling in Tests

Authentication tests handle Spring Security carefully to prevent test flakiness caused by security filters instead of business logic.

Applied strategies:

- `@AutoConfigureMockMvc(addFilters = false)` is used in integration tests to disable security filter chain interference for controller behavior verification
- Security collaborators (`AuthenticationManager`, `JwtUtil`) are mocked with `@MockitoBean`
- Existing project tests also demonstrate role simulation via `@WithMockUser` in controller-level security-aware scenarios

Why this is needed:

- Keeps tests deterministic
- Focuses each test on its intended layer (service logic vs. endpoint behavior)
- Avoids false negatives caused by unrelated authentication filters

---

## 7. Test Execution

Run all tests:

```bash
./mvnw test
```

Run only authentication test classes:

```bash
./mvnw "-Dtest=UserServiceTest,AuthServiceTest,AuthControllerIntegrationTest" test
```

Observed targeted execution result:

- Tests run: 16
- Failures: 0
- Errors: 0
- Build: SUCCESS

CI/CD readiness:

- Maven command is CI-compatible
- Tests are deterministic (mocked dependencies + H2 test profile)
- No external service dependency is required for auth test execution

---

## 8. Example Test Cases

### Example Unit Test (UserService)

Purpose: verify registration encodes password and persists normalized user data.

```java
@Test
@DisplayName("Given valid registration data when registerUser then user is saved with encoded password")
void givenValidRegistrationData_whenRegisterUser_thenUserIsSavedWithEncodedPassword() {
    when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
    when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));
    when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User saved = userService.registerUser("Alice", "Alice@Example.com", "secret123", "book_friend");

    assertEquals("alice@example.com", saved.getEmail());
    assertEquals("encoded-secret", saved.getPassword());
}
```

### Example Unit Test (Auth/UserDetails)

Purpose: verify missing user causes authentication lookup failure.

```java
@Test
@DisplayName("Given unknown email when loadUserByUsername then username not found is thrown")
void givenUnknownEmail_whenLoadUserByUsername_thenUsernameNotFoundIsThrown() {
    when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername("missing@example.com"));
}
```

### Example Integration Test (Auth API)

Purpose: verify login success returns JWT token payload.

```java
@Test
@DisplayName("Given valid credentials when posting api login then returns jwt token")
void givenValidCredentials_whenPostingApiLogin_thenReturnsJwtToken() throws Exception {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
    when(jwtUtil.generateToken("newuser@example.com")).thenReturn("mock-jwt-token");

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"newuser@example.com","password":"secret123"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("mock-jwt-token"));
}
```

---

## 9. Challenges and Solutions

### Challenge 1: Security filters blocking endpoint tests

- Issue: authentication/authorization filters can fail requests before controller logic is tested
- Solution: `@AutoConfigureMockMvc(addFilters = false)` for targeted controller behavior tests

### Challenge 2: Login failure assertion behavior

- Issue: mocked `AuthenticationManager` throwing `BadCredentialsException` surfaces as servlet exception during MockMvc execution
- Solution: use `assertThrows(ServletException.class, ...)` in wrong-password integration case

### Challenge 3: Test context serialization bean assumptions

- Issue: relying on context-injected `ObjectMapper` can fail in specific test slices/environments
- Solution: use explicit JSON payload strings for simple request bodies in integration tests

### Challenge 4: Database dependency in tests

- Issue: authentication tests should not depend on external PostgreSQL runtime
- Solution: `application-test.yaml` uses H2 in-memory database and disables Docker compose for test profile

---

## 10. Conclusion

Authentication testing in this project provides strong confidence across both business logic and API behavior.

- Unit tests validate core registration, role resolution, password handling, and user-details loading logic
- Integration tests verify controller-level authentication flows and error behavior
- Security-aware test setup ensures stable, CI-ready execution

This layered approach increases system reliability, reduces regression risk in security-critical paths, and provides clear evidence for viva, review, and production-readiness discussions.

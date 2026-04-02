# Admin Testing README

Date: 2 April 2026

## 1. Purpose
This document summarizes admin-level testing added for the project.

Admin features are high-risk because they can modify core data (books, offers, users, exchanges). These tests ensure admin operations are correct, secure, and stable.

## 2. Why Admin Tests Are Needed
- Prevent regressions in critical admin actions.
- Validate role-based access to admin endpoints.
- Ensure admin responses are consistently wrapped in API response format.
- Protect ownership/authorization rules and failure handling.
- Verify admin-facing repository queries return correctly ordered and complete data.

## 3. Admin Test Coverage by Layer

### Service layer tests
File:
- src/test/java/com/example/project/admin/service/AdminBookServiceTest.java
- src/test/java/com/example/project/admin/service/AdminOfferServiceTest.java
- src/test/java/com/example/project/admin/service/AdminUserServiceTest.java

What they validate:
- Book create/update/delete service logic, validation, and conflict handling.
- Offer block/delete service behavior and conflict/not-found handling.
- User blocking logic, including self-block prevention and status mapping.

### Controller integration tests
File:
- src/test/java/com/example/project/admin/controller/AdminControllerIntegrationTest.java

What it validates:
- GET/POST admin endpoint behavior under Spring context.
- Success and error response envelope (success/message/data).
- Role-based protection behavior for non-admin access attempts.

### Repository layer tests
File:
- src/test/java/com/example/project/admin/repository/AdminRepositoryLayerTest.java

What it validates:
- User role-based repository query for admin retrieval.
- Offer repository admin listing query with details and ordering.
- Exchange request repository admin listing query with details and ordering.

## 4. Test Design Approach
- JUnit 5 is used for all tests.
- Mockito is used for unit/service and controller dependency isolation.
- MockMvc is used for admin controller integration behavior.
- Data JPA tests are used for repository query correctness.
- Active test profile is used to keep tests deterministic.

## 5. How to Run Admin Tests

### Run all admin tests added
```bash
./mvnw -Dtest=AdminBookServiceTest,AdminOfferServiceTest,AdminUserServiceTest,AdminControllerIntegrationTest,AdminRepositoryLayerTest test
```

### Run admin service tests only
```bash
./mvnw -Dtest=AdminBookServiceTest,AdminOfferServiceTest,AdminUserServiceTest test
```

### Run admin controller integration tests only
```bash
./mvnw -Dtest=AdminControllerIntegrationTest test
```

### Run admin repository tests only
```bash
./mvnw -Dtest=AdminRepositoryLayerTest test
```

### Run full project tests
```bash
./mvnw test
```

## 6. Verified Results (Current Session)
- AdminUserServiceTest + AdminControllerIntegrationTest: PASS
- AdminRepositoryLayerTest: PASS

## 7. Notes
- Admin controller tests currently assert behavior as implemented with global admin exception handling.
- Repository tests use test database configuration compatible with existing project constraints.
- These tests are designed to be CI-friendly and run without manual setup.

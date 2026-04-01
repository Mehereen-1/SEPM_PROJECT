# Book and Offer Testing Report

Date: 2 April 2026

## Scope
This document summarizes the testing added for book and offer features.

## Why These Tests Are Needed
- Protect core product flows from regressions (book browse/create and offer create/manage).
- Validate authorization and ownership rules before release.
- Verify API responses used by frontend pages and scripts remain stable.
- Catch edge cases early, such as duplicate books, missing records, and invalid input.

## How These Tests Work
The test suite uses 3 layers so each type of risk is validated at the right level:

### 1. Service tests (fast business-logic checks)
- Framework: JUnit 5 + Mockito.
- Goal: validate logic without HTTP or DB setup.
- Examples: duplicate checks, validation branches, save/delete delegation.

### 2. Repository tests (query correctness)
- Framework: @DataJpaTest.
- Goal: verify query filtering and ordering behavior.
- Examples: offer lookup by status, by user, and by user+status.

### 3. Controller integration tests (API behavior)
- Framework: SpringBootTest + MockMvc with mocked dependencies.
- Goal: validate endpoint contract and status codes.
- Examples: 200/201 success paths and 400/401/403/404/409 error paths.

## Added Test Files

### Book domain
- [src/test/java/com/example/project/service/BookServiceTest.java](src/test/java/com/example/project/service/BookServiceTest.java)
- [src/test/java/com/example/project/admin/service/AdminBookServiceTest.java](src/test/java/com/example/project/admin/service/AdminBookServiceTest.java)
- [src/test/java/com/example/project/controller/BookControllerIntegrationTest.java](src/test/java/com/example/project/controller/BookControllerIntegrationTest.java)

### Offer domain
- [src/test/java/com/example/project/admin/service/AdminOfferServiceTest.java](src/test/java/com/example/project/admin/service/AdminOfferServiceTest.java)
- [src/test/java/com/example/project/repository/OfferRepositoryTest.java](src/test/java/com/example/project/repository/OfferRepositoryTest.java)
- [src/test/java/com/example/project/controller/OfferControllerIntegrationTest.java](src/test/java/com/example/project/controller/OfferControllerIntegrationTest.java)

## Coverage Summary

### Book service and admin service coverage
- Duplicate prevention on create.
- Required-field validation paths.
- Successful create, update, and delete paths.
- Not-found and conflict responses.
- Search/delegation and count behavior.

### Book controller integration coverage
- Browse endpoint returns list.
- Get-by-id success and not-found.
- Create endpoint success for admin role.
- Duplicate create conflict handling.
- Non-admin create denied.

### Offer repository and admin service coverage
- Query methods with expected filtering and ordering.
- Admin offer list mapping.
- Block offer flow.
- Delete offer success, not-found, and conflict handling.

### Offer controller integration coverage
- Browse active offers response mapping.
- Create offer validation, not-found, unauthorized, and success paths.
- My active offers unauthorized and success paths.
- Update ownership enforcement.
- Delete ownership enforcement and success path.

## How to Run

### Run all tests
- ./mvnw test

### Run only book and offer focused suite
- ./mvnw -Dtest=BookServiceTest,AdminBookServiceTest,BookControllerIntegrationTest,AdminOfferServiceTest,OfferRepositoryTest,OfferControllerIntegrationTest test

### Run only book tests
- ./mvnw -Dtest=BookServiceTest,AdminBookServiceTest,BookControllerIntegrationTest test

### Run only offer tests
- ./mvnw -Dtest=AdminOfferServiceTest,OfferRepositoryTest,OfferControllerIntegrationTest test

### Run a single class
- ./mvnw -Dtest=OfferControllerIntegrationTest test

### Run a single test method
- ./mvnw -Dtest=OfferControllerIntegrationTest#givenValidRequest_whenCreateOffer_thenCreated test

## Commands Previously Used During Verification
- ./mvnw -Dtest=BookControllerIntegrationTest test
- ./mvnw -Dtest=OfferControllerIntegrationTest test
- ./mvnw -Dtest=BookServiceTest,AdminBookServiceTest,AdminOfferServiceTest,OfferRepositoryTest test -DskipITs

## Latest Verified Results
- BookControllerIntegrationTest: 6 tests, 0 failures, 0 errors.
- OfferControllerIntegrationTest: 9 tests, 0 failures, 0 errors.
- Focused book and offer suite: 25 tests, 0 failures, 0 errors.

## Notes
- Offer repository data tests use non-replaced test database configuration to align with project constraints behavior.

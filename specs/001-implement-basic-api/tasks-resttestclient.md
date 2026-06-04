# RestTestClient Upgrade Implementation Tasks

**Feature**: Replace `@RestClientTest` + `MockRestServiceServer` with modern **RestTestClient** fluent testing API  
**Branch**: `003-resttestclient-upgrade`  
**Created**: June 1, 2026  
**Status**: Ready for Implementation  
**Total Tasks**: 62 | **Phase 1**: 3 | **Phase 2**: 12 | **Phase 3**: 18 | **Phase 4**: 5 | **Phase 5**: 15 | **Phase 6**: 9

---

## Implementation Strategy

**MVP Scope**: Phase 1 → Phase 2 → Phase 3 → Phase 5 (Critical path for fastest deployment)
- Delivers complete test migration from MockRestServiceServer to RestTestClient
- Improves code readability and maintainability
- Aligns with Spring Boot 4.x best practices
- **Estimated timeline**: 2-3 weeks (13-15 hours of development)

**Recommended Execution Path**:
1. **Phase 1** (Verification) — Establish environment baseline [0 dependencies, parallelizable]
2. **Phase 2** (Unit Test Refactoring) — Migrate EntityClientTest + ErrorHandlingTest [1 dependency: Phase 1]
3. **Phase 3** (Integration Test Refactoring) — Migrate 3 integration test files [1 dependency: Phase 2]
4. **Phase 4** (Helper Utilities - Optional) — Implement testing utilities [parallel with Phase 3]
5. **Phase 5** (Validation) — Run complete test suite, verify coverage [2 dependencies: Phase 2-3]
6. **Phase 6** (Documentation) — Update all documentation [1 dependency: Phase 5]

**Parallel Opportunities**:
- **Within Phase 2**: ErrorHandlingTest (T009-T010) can run in parallel with EntityClientTest (T005-T008) after Phase 1
- **Within Phase 3**:
  - ServiceToServiceAuthIntegrationTest (T019-T028) can run in parallel with ChaosEngineeringTest (T029-T036)
  - KeycloakChaosTest refactoring (T037-T045) can run in series or parallel depending on team size
- **Phase 4**: Helper utilities development can run parallel with Phase 3 integration test refactoring
- **Testing**: Unit and integration test execution can run in parallel during Phase 5

---

## Dependency Graph

```
Phase 1: Verification (Baseline - Zero dependencies)
    ↓
Phase 2: Unit Test Refactoring (depends on Phase 1)
    ↓
Phase 3: Integration Test Refactoring (depends on Phase 2)
    ↓
Phase 4: Helper Utilities (OPTIONAL - can run parallel with Phase 3)
    ↓ ↓
Phase 5: Validation (depends on Phase 2 & 3 completion)
    ↓
Phase 6: Documentation (depends on Phase 5 completion)
```

**Critical Path**: Phase 1 → Phase 2 → Phase 3 → Phase 5 → Phase 6 (13-15 hours)
**Optional Parallel**: Phase 4 (1 hour, can reduce critical path if resources available)

---

## Test File Summary

| Test File | Location | Scenarios | Phase | Story Points |
|-----------|----------|-----------|-------|--------------|
| EntityClientTest | src/test/java/com/agilesolutions/service_a/unit/ | 12 | 2 | 5 |
| ErrorHandlingTest | src/test/java/com/agilesolutions/service_a/unit/ | 5 | 2 | 3 |
| ServiceToServiceAuthIntegrationTest | src/test/java/com/agilesolutions/service_a/integration/ | 10 | 3 | 5 |
| ChaosEngineeringTest | src/test/java/com/agilesolutions/service_a/integration/ | 8 | 3 | 5 |
| KeycloakChaosTest | src/test/java/com/agilesolutions/service_a/integration/ | 9 | 3 | 5 |
| **TOTAL** | — | **44** | — | **23** |

---

## Phase 1: Verification (Baseline Environment)

**Phase Goal**: Verify Spring Boot 4.0.5 environment and confirm RestTestClient bean auto-configuration

**Story Points**: 2  
**Estimated Hours**: 1 hour  
**Dependencies**: None (zero dependencies)

**Independent Test Criteria**:
- ✅ RestTestClient bean is available for auto-wiring in @RestClientTest classes
- ✅ Spring Boot 4.0.5 classpath includes spring-test with RestTestClient support
- ✅ All test classes currently compile without errors
- ✅ Existing test suite runs (baseline)

### Verification Tasks

- [x] T001 Verify RestTestClient bean auto-configuration in Spring Boot 4.0.5 with `./gradlew test --tests "*ClientTest" -Dtest.single=EntityClientTest` and confirm no dependency errors

- [x] T002 [P] Check build.gradle dependencies for spring-boot-starter-test:4.0.5 and confirm includes spring-test:6.1.x in `apps/service-a/build.gradle`

- [x] T003 [P] Run baseline test suite with `./gradlew test` from repository root and document current test count (should be 44 tests passing with @RestClientTest pattern)

---

## Phase 2: Unit Test Refactoring

**Phase Goal**: Refactor unit test files (EntityClientTest, ErrorHandlingTest) to use RestTestClient fluent API

**Story Points**: 8  
**Estimated Hours**: 3 hours  
**Dependencies**: Phase 1 complete

**Independent Test Criteria**:
- ✅ All 17 unit test scenarios pass with RestTestClient (12 + 5)
- ✅ No MockRestServiceServer references remain in EntityClientTest
- ✅ No MockRestServiceServer references remain in ErrorHandlingTest
- ✅ Zero compilation errors and no deprecation warnings
- ✅ All assertions follow RestTestClient fluent API pattern
- ✅ Test execution time comparable to baseline

### 2.1 EntityClientTest Refactoring (12 scenarios)

**Test File**: `src/test/java/com/agilesolutions/service_a/unit/EntityClientTest.java`

**Current Pattern**:
```java
@SpringBootTest
@RestClientTest(EntityClient.class)
class EntityClientTest {
    @Autowired private MockRestServiceServer mockServer;
    @Test void testFetchEntity() {
        mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
            .andRespond(withSuccess(json(expected), APPLICATION_JSON));
        EntityInfo result = entityClient.getEntityInfo(id);
        mockServer.verify();
    }
}
```

**Transformation Pattern**:
```java
@SpringBootTest
@RestClientTest(EntityClient.class)
class EntityClientTest {
    @Autowired private RestTestClient restTestClient;
    @Test void testFetchEntity() {
        restTestClient.get()
            .uri("/api/internal/info/{id}", id)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody(EntityInfo.class)
            .satisfies(result -> assertThat(result).isEqualTo(expected));
    }
}
```

#### EntityClientTest Scenarios

- [ ] T004 [P] [UNIT-EC] Replace MockRestServiceServer with RestTestClient in EntityClientTest class header and field injection in `src/test/java/com/agilesolutions/service_a/unit/EntityClientTest.java` (remove @Autowired MockRestServiceServer, add @Autowired RestTestClient)

- [ ] T005 [P] [UNIT-EC] Refactor scenario 1: Successful entity retrieval with OAuth2 token - transform mockServer.expect() chains to restTestClient.get().uri().header().exchange().expectStatus().isOk().expectBody(EntityInfo.class).satisfies() pattern in EntityClientTest

- [ ] T006 [P] [UNIT-EC] Refactor scenario 2: Entity not found (404) error handling - transform to restTestClient.get().uri().exchange().expectStatus().isNotFound().expectBody().json() pattern

- [ ] T007 [P] [UNIT-EC] Refactor scenario 3: Service unavailable (503) handling - transform to expectStatus().is5xxServerError() assertions in EntityClientTest

- [ ] T008 [P] [UNIT-EC] Refactor scenario 4: Connection timeout retry logic - update retry mock setup to use RestTestClient timeout assertions and verify retry behavior with `.satisfies(result -> assertThat(...).isNotNull())`

- [ ] T009 [P] [UNIT-EC] Refactor scenarios 5-8 in EntityClientTest: (5) 500 Internal Server Error, (6) OAuth2 Bearer token inclusion, (7) Request header validation, (8) Entity retrieval by name - update all to RestTestClient fluent API with proper header assertions and body parsing

- [ ] T010 [P] [UNIT-EC] Refactor scenarios 9-12 in EntityClientTest: (9) Maximum retries exceeded, (10) Malformed JSON response handling, (11) Gateway timeout, (12) Entity response parsing with special characters - add `.expectBody(EntityInfo.class).satisfies(...)` for response parsing and character encoding validation

- [ ] T011 [P] [UNIT-EC] Add assertion helper methods in EntityClientTest for common patterns: `assertValidEntityResponse()`, `assertErrorResponse()` to improve code reuse across 12 scenarios

- [ ] T012 [UNIT-EC] Compile EntityClientTest with `./gradlew compileTestJava` and verify zero errors and warnings - all 12 test methods use RestTestClient fluent API

- [ ] T013 [UNIT-EC] Run EntityClientTest with `./gradlew test --tests "EntityClientTest"` and verify all 12 scenarios pass (100% success rate, no timeout failures)

### 2.2 ErrorHandlingTest Refactoring (5 scenarios)

**Test File**: `src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java`

#### ErrorHandlingTest Scenarios

- [ ] T014 [P] [UNIT-EH] Replace MockRestServiceServer with RestTestClient in ErrorHandlingTest class field injection and update class annotations in `src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java`

- [ ] T015 [P] [UNIT-EH] Refactor scenario 1: 404 Not Found handling - transform mockServer.expect(HttpStatus.NOT_FOUND) to restTestClient.get().uri().exchange().expectStatus().isNotFound()

- [ ] T016 [P] [UNIT-EH] Refactor scenario 2: 401 Unauthorized handling - transform to expectStatus().isUnauthorized() assertion and verify error response body with `.expectBody().json()`

- [ ] T017 [P] [UNIT-EH] Refactor scenario 3: 500 Internal Server Error - transform to expectStatus().is5xxServerError() with error response parsing

- [ ] T018 [P] [UNIT-EH] Refactor scenarios 4-5: (4) 503 Service Unavailable, (5) Connection timeout - add timeout assertions and service unavailable status checks using RestTestClient

- [ ] T019 [UNIT-EH] Compile ErrorHandlingTest with `./gradlew compileTestJava` and verify zero errors - all 5 scenarios refactored to RestTestClient

- [ ] T020 [UNIT-EH] Run ErrorHandlingTest with `./gradlew test --tests "ErrorHandlingTest"` and verify all 5 error scenarios pass successfully

---

## Phase 3: Integration Test Refactoring

**Phase Goal**: Refactor 3 integration test files (ServiceToServiceAuthIntegrationTest, ChaosEngineeringTest, KeycloakChaosTest) to use RestTestClient

**Story Points**: 15  
**Estimated Hours**: 4 hours  
**Dependencies**: Phase 2 complete (Unit Test Refactoring)

**Independent Test Criteria**:
- ✅ All 27 integration test scenarios pass (10 + 8 + 9)
- ✅ No MockRestServiceServer references in any integration test files
- ✅ OAuth2 token mocking works correctly with RestTestClient
- ✅ Chaos engineering test assertions compatible with RestTestClient
- ✅ Zero compilation errors and no deprecation warnings
- ✅ Integration test execution time within acceptable range

### 3.1 ServiceToServiceAuthIntegrationTest Refactoring (10 scenarios)

**Test File**: `src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java`

#### ServiceToServiceAuthIntegrationTest Scenarios

- [ ] T021 [P] [INT-STA] Replace MockRestServiceServer with RestTestClient in ServiceToServiceAuthIntegrationTest class in `src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java` - update field injection and annotations

- [ ] T022 [P] [INT-STA] Refactor scenario 1: Valid OAuth2 token acceptance - transform mockServer expect/andRespond chains to restTestClient.get().header("Authorization", "Bearer " + validToken).exchange().expectStatus().isOk()

- [ ] T023 [P] [INT-STA] Refactor scenario 2: Missing token rejection - add test for request without Authorization header using `.exchange().expectStatus().isUnauthorized()`

- [ ] T024 [P] [INT-STA] Refactor scenario 3: Expired token handling - implement RestTestClient test with expired token JWT and verify 401 response with `.satisfies(response -> assertThat(...).contains("expired"))`

- [ ] T025 [P] [INT-STA] Refactor scenario 4: Invalid token format rejection - test malformed JWT format and verify rejection using `.expectStatus().isUnauthorized()`

- [ ] T026 [P] [INT-STA] Refactor scenarios 5-7: (5) Malformed Authorization header, (6) OAuth2 scope validation, (7) Service B 401/403 error handling - update all to RestTestClient fluent API with header and body assertions

- [ ] T027 [P] [INT-STA] Refactor scenarios 8-10: (8) Concurrent request handling, (9) Stateless authorization validation, (10) Token caching behavior - implement with RestTestClient and add `.satisfies()` for concurrent request assertions

- [ ] T028 [INT-STA] Compile ServiceToServiceAuthIntegrationTest with `./gradlew compileTestJava` and verify zero errors - all 10 OAuth2 scenarios refactored

- [ ] T029 [INT-STA] Run ServiceToServiceAuthIntegrationTest with `./gradlew test --tests "ServiceToServiceAuthIntegrationTest"` and verify all 10 authentication scenarios pass

### 3.2 ChaosEngineeringTest Refactoring (8 scenarios)

**Test File**: `src/test/java/com/agilesolutions/service_a/integration/ChaosEngineeringTest.java`

#### ChaosEngineeringTest Scenarios

- [ ] T030 [P] [INT-CE] Replace MockRestServiceServer with RestTestClient in ChaosEngineeringTest class in `src/test/java/com/agilesolutions/service_a/integration/ChaosEngineeringTest.java`

- [ ] T031 [P] [INT-CE] Refactor scenario 1: Network timeout simulation - update mockServer timeout setup to RestTestClient with timeout expectations and retry logic assertions

- [ ] T032 [P] [INT-CE] Refactor scenario 2: Service unavailability (503) - transform to restTestClient.get().exchange().expectStatus().is5xxServerError() with automatic retry verification

- [ ] T033 [P] [INT-CE] Refactor scenario 3: Circuit breaker state transitions - update circuit breaker mock setup to verify with RestTestClient state change assertions and `.satisfies()` custom checks

- [ ] T034 [P] [INT-CE] Refactor scenario 4: Retry backoff validation - implement RestTestClient assertions for exponential backoff timing with `.satisfies(result -> assertThat(elapsedTime).isGreaterThanOrEqualTo(expectedBackoff))`

- [ ] T035 [P] [INT-CE] Refactor scenarios 5-7: (5) Exponential backoff timing, (6) Cascading failure handling, (7) Transient failure recovery - apply RestTestClient fluent API with retry timing assertions

- [ ] T036 [P] [INT-CE] Refactor scenario 8: Concurrent load resilience - test multiple concurrent RestTestClient requests and verify all complete successfully with `.satisfies()` concurrency assertions

- [ ] T037 [INT-CE] Compile ChaosEngineeringTest with `./gradlew compileTestJava` and verify zero errors - all 8 chaos scenarios refactored to RestTestClient

- [ ] T038 [INT-CE] Run ChaosEngineeringTest with `./gradlew test --tests "ChaosEngineeringTest"` and verify all 8 resilience scenarios pass (account for retry delays)

### 3.3 KeycloakChaosTest Refactoring (9 scenarios)

**Test File**: `src/test/java/com/agilesolutions/service_a/integration/KeycloakChaosTest.java`

#### KeycloakChaosTest Scenarios

- [ ] T039 [P] [INT-KC] Replace MockRestServiceServer with RestTestClient in KeycloakChaosTest class in `src/test/java/com/agilesolutions/service_a/integration/KeycloakChaosTest.java` - update Keycloak token mock setup

- [ ] T040 [P] [INT-KC] Refactor scenario 1: Keycloak unavailability - transform Keycloak mock server failure to RestTestClient request with `.exchange().expectStatus().is5xxServerError()` when Keycloak unavailable

- [ ] T041 [P] [INT-KC] Refactor scenario 2: Keycloak degradation - simulate slow Keycloak responses and verify timeout behavior with RestTestClient timeout assertions

- [ ] T042 [P] [INT-KC] Refactor scenario 3: Token endpoint timeout - update token request mocking to use RestTestClient with timeout assertions and automatic retry

- [ ] T043 [P] [INT-KC] Refactor scenario 4: Token refresh failure - transform mockServer token refresh failure simulation to RestTestClient with error response handling

- [ ] T044 [P] [INT-KC] Refactor scenarios 5-7: (5) Token acquisition retry, (6) OAuth2 error handling, (7) Multi-token concurrent requests - implement RestTestClient assertions for token retry logic and concurrent token requests

- [ ] T045 [P] [INT-KC] Refactor scenarios 8-9: (8) Token invalidation recovery, (9) Cascade failure with token refresh - add RestTestClient test cases for token invalidation and cascading failure scenarios with `.satisfies()` recovery assertions

- [ ] T046 [INT-KC] Compile KeycloakChaosTest with `./gradlew compileTestJava` and verify zero errors - all 9 Keycloak chaos scenarios refactored

- [ ] T047 [INT-KC] Run KeycloakChaosTest with `./gradlew test --tests "KeycloakChaosTest"` and verify all 9 Keycloak chaos scenarios pass successfully

---

## Phase 4: Helper Utilities Implementation (Optional)

**Phase Goal**: Create reusable test helper utilities to reduce boilerplate in RestTestClient tests

**Story Points**: 3  
**Estimated Hours**: 1 hour  
**Dependencies**: Phase 2 complete (can run parallel with Phase 3)

**Independent Test Criteria**:
- ✅ Helper class compiles without errors
- ✅ All utility methods are reusable across test classes
- ✅ Helper method usage adopted in at least 5 tests
- ✅ Code coverage for helper class ≥ 80%

### Helper Utilities Tasks

- [ ] T048 [P] [HELPER] Create RestTestClientHelper.java in `src/test/java/com/agilesolutions/service_a/test/RestTestClientHelper.java` with utility methods for common RestTestClient patterns

- [ ] T049 [P] [HELPER] Implement `createMockAuthorizedClient(String token)` method for OAuth2 client creation in RestTestClientHelper.java

- [ ] T050 [P] [HELPER] Implement `assertValidEntityResponse(EntityInfo expected)` helper method returning Consumer<EntityInfo> for standard entity assertions in RestTestClientHelper.java

- [ ] T051 [P] [HELPER] Implement `setDefaultHeaders(String token)` helper method returning Consumer<HttpHeaders> for common header setup in RestTestClientHelper.java

- [ ] T052 [HELPER] Refactor EntityClientTest and ErrorHandlingTest to use RestTestClientHelper utility methods and verify compilation and test execution in `src/test/java/com/agilesolutions/service_a/unit/`

---

## Phase 5: Validation & Testing

**Phase Goal**: Run complete test suite, verify coverage, and validate all RestTestClient migrations

**Story Points**: 8  
**Estimated Hours**: 2 hours  
**Dependencies**: Phase 2 complete, Phase 3 complete

**Independent Test Criteria**:
- ✅ All 44 test scenarios pass (17 unit + 27 integration)
- ✅ `./gradlew compileTestJava` succeeds with zero errors and zero warnings
- ✅ Code coverage maintained or improved (target: 85%+ for RestClient package)
- ✅ No MockRestServiceServer references in any test files
- ✅ No deprecation warnings in test compilation
- ✅ Test execution time is acceptable (unit tests < 30s, integration tests < 60s)

### Validation Tasks

- [ ] T053 [VALIDATE] Run `./gradlew compileTestJava` from repository root and verify zero compilation errors and warnings for all test classes listed in RESTTESTCLIENT-UPGRADE-PLAN.md

- [ ] T054 [VALIDATE] Run `./gradlew test` to execute all unit tests and verify all 17 unit test scenarios pass (EntityClientTest: 12 + ErrorHandlingTest: 5) with completion time logged

- [ ] T055 [VALIDATE] Run `./gradlew integrationTest` to execute all integration tests and verify all 27 integration test scenarios pass (ServiceToServiceAuthIntegrationTest: 10 + ChaosEngineeringTest: 8 + KeycloakChaosTest: 9)

- [ ] T056 [VALIDATE] Execute `./gradlew jacocoTestReport` and verify code coverage for RestClient-related classes meets 85%+ target using `build/reports/jacoco/test/html/index.html`

- [ ] T057 [VALIDATE] Search for remaining MockRestServiceServer references in test files using `grep -r "MockRestServiceServer" apps/service-a/src/test/` and verify result is empty (zero matches)

- [ ] T058 [VALIDATE] Search for deprecation warnings in test compilation output and verify no `@Deprecated` method calls in test files using `grep -r "@Deprecated" apps/service-a/src/test/java/com/agilesolutions/service_a/unit/` and `grep -r "@Deprecated" apps/service-a/src/test/java/com/agilesolutions/service_a/integration/`

- [ ] T059 [VALIDATE] Verify RestTestClient imports are correct in all 5 test files: `grep -r "org.springframework.test.web.client.RestTestClient" apps/service-a/src/test/` should show 5 files with correct import

- [ ] T060 [VALIDATE] Create validation report at `specs/001-implement-basic-api/RESTTESTCLIENT-VALIDATION-REPORT.md` documenting:
  - Test execution summary (44/44 scenarios passing)
  - Code coverage metrics (baseline vs. post-refactor)
  - Compilation results (zero errors/warnings)
  - MockRestServiceServer reference scan results
  - Performance metrics (test execution times)
  - Any issues encountered and their resolution

---

## Phase 6: Documentation Update

**Phase Goal**: Update all project documentation to reflect RestTestClient migration and usage patterns

**Story Points**: 4  
**Estimated Hours**: 1 hour  
**Dependencies**: Phase 5 complete

**Independent Test Criteria**:
- ✅ RESTCLIENT-IMPLEMENTATION.md includes RestTestClient section
- ✅ README.md has RestTestClient usage guide with example code
- ✅ RESTCLIENT-MIGRATION.md updated with Phase 3 completion notes
- ✅ All test files have Javadoc explaining RestTestClient patterns
- ✅ Release notes prepared for v7.2.0 with RestTestClient migration details

### Documentation Tasks

- [ ] T061 [P] [DOC] Update `specs/001-implement-basic-api/RESTCLIENT-IMPLEMENTATION.md` - Add new section "RestTestClient Fluent Testing API" with before/after code examples showing transformation from MockRestServiceServer to RestTestClient patterns (copy examples from RESTTESTCLIENT-UPGRADE-PLAN.md)

- [ ] T062 [P] [DOC] Create "RestTestClient Best Practices" section in RESTCLIENT-IMPLEMENTATION.md documenting:
  - Fluent API usage patterns (get/post/exchange/expectStatus chains)
  - Header assertion methods (.expectHeader().contentType())
  - Body parsing with .expectBody(Type.class).satisfies()
  - Error handling scenarios (4xx/5xx status assertions)
  - Common pitfalls and how RestTestClient avoids them

- [ ] T063 [P] [DOC] Update `specs/001-implement-basic-api/RESTCLIENT-MIGRATION.md` - Add Phase 3 (RestTestClient Migration) section with:
  - Completion date and summary (44/44 scenarios refactored)
  - Test execution statistics (all passing, coverage metrics)
  - Dependencies and setup required
  - Configuration changes (none required, auto-configured)
  - Known limitations or workarounds

- [ ] T064 [P] [DOC] Update `README.md` at repository root - Add "RestTestClient Testing Guide" section with:
  - Quick start example (copy from RESTTESTCLIENT-UPGRADE-PLAN.md Modern Pattern)
  - Link to RESTCLIENT-IMPLEMENTATION.md
  - Link to Spring Boot RestTestClient documentation
  - Basic assertions reference (expectStatus, expectBody, satisfies)

- [ ] T065 [DOC] Add Javadoc comments to EntityClientTest.java explaining RestTestClient fluent API usage with at least 3 code examples for different assertion types in `src/test/java/com/agilesolutions/service_a/unit/EntityClientTest.java`

- [ ] T066 [DOC] Add Javadoc comments to ErrorHandlingTest.java documenting error scenario testing with RestTestClient and 2+ examples in `src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java`

- [ ] T067 [DOC] Add Javadoc comments to ServiceToServiceAuthIntegrationTest.java explaining OAuth2 token handling and RestTestClient assertions in `src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java`

- [ ] T068 [DOC] Add Javadoc comments to ChaosEngineeringTest.java and KeycloakChaosTest.java documenting resilience testing patterns with RestTestClient in `src/test/java/com/agilesolutions/service_a/integration/` (both files)

- [ ] T069 [DOC] Create/Update release notes for v7.2.0 at `RELEASE-NOTES-v7.2.0.md` documenting:
  - Feature: RestTestClient migration (Spring Boot 4.0+ modernization)
  - 44 test scenarios refactored from @RestClientTest + MockRestServiceServer → RestTestClient
  - Benefits: Improved readability, fluent API, aligned with Spring Boot 4.x patterns
  - Migration path: Automatic, no production code changes required
  - Breaking changes: None (internal test changes only)

---

## Success Criteria Summary

### Code Quality Metrics
| Metric | Target | Validation Task |
|--------|--------|-----------------|
| Test scenarios refactored | 44/44 (100%) | T054, T055 |
| MockRestServiceServer references removed | 0 | T057 |
| Compilation errors | 0 | T053 |
| Deprecation warnings | 0 | T058 |
| Code coverage | 85%+ | T056 |
| Test execution time | Baseline comparable | T054, T055 |

### Test Coverage Breakdown
| Test File | Scenarios | Status Task | Expected Result |
|-----------|-----------|------------|-----------------|
| EntityClientTest | 12 | T013 | All pass ✅ |
| ErrorHandlingTest | 5 | T020 | All pass ✅ |
| ServiceToServiceAuthIntegrationTest | 10 | T029 | All pass ✅ |
| ChaosEngineeringTest | 8 | T038 | All pass ✅ |
| KeycloakChaosTest | 9 | T047 | All pass ✅ |
| **TOTAL** | **44** | **T060** | **44/44 pass ✅** |

### Deployment Readiness
| Criterion | Status Task | Requirement |
|-----------|------------|-------------|
| All tests passing | T060 | 44/44 scenarios green |
| Code review ready | Phase 6 | Documentation complete |
| Release tag prepared | T069 | v7.2.0 notes ready |
| Rollback plan | N/A | Git revert capability confirmed |

---

## Code Pattern Reference

### Transformation Patterns by Scenario Type

#### Pattern 1: Simple GET Request
**BEFORE** (MockRestServiceServer):
```java
mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", containsString("Bearer")))
        .andRespond(withSuccess(json(expectedEntity), APPLICATION_JSON));

EntityInfo result = entityClient.getEntityInfo(id);

assertThat(result).isEqualTo(expectedEntity);
mockServer.verify();
```

**AFTER** (RestTestClient):
```java
restTestClient.get()
        .uri("/api/internal/info/{id}", id)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().isOk()
        .expectBody(EntityInfo.class)
        .satisfies(result -> assertThat(result).isEqualTo(expectedEntity));
```

#### Pattern 2: Error Handling (4xx/5xx)
**BEFORE** (MockRestServiceServer):
```java
mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
        .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .body("{\"error\": \"Not Found\"}"));

assertThrows(HttpClientErrorException.NotFound.class, 
        () -> entityClient.getEntityInfo(id));
mockServer.verify();
```

**AFTER** (RestTestClient):
```java
restTestClient.get()
        .uri("/api/internal/info/{id}", id)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .json("{\"error\": \"Not Found\"}");
```

#### Pattern 3: Complex Assertions with Headers
**BEFORE** (MockRestServiceServer):
```java
mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
        .andExpect(header("Authorization", "Bearer " + token))
        .andExpect(header("Content-Type", "application/json"))
        .andRespond(withSuccess(json(expected), APPLICATION_JSON));

EntityInfo result = entityClient.getEntityInfo(id);

assertThat(result.getId()).isEqualTo(expected.getId());
assertThat(result.getName()).isEqualTo(expected.getName());
mockServer.verify();
```

**AFTER** (RestTestClient):
```java
restTestClient.get()
        .uri("/api/internal/info/{id}", id)
        .header("Authorization", "Bearer " + token)
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(EntityInfo.class)
        .satisfies(result -> {
            assertThat(result.getId()).isEqualTo(expected.getId());
            assertThat(result.getName()).isEqualTo(expected.getName());
        });
```

#### Pattern 4: OAuth2 Token Scenarios
**BEFORE** (MockRestServiceServer):
```java
mockServer.expect(header("Authorization", "Bearer " + expiredToken))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .body("{\"error_description\": \"Token expired\"}"));

assertThrows(HttpClientErrorException.Unauthorized.class,
        () -> entityClient.getEntityInfoWithAuth(id, expiredToken));
```

**AFTER** (RestTestClient):
```java
restTestClient.get()
        .uri("/api/internal/info/{id}", id)
        .header("Authorization", "Bearer " + expiredToken)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .json("{\"error_description\": \"Token expired\"}");
```

#### Pattern 5: Timeout & Retry Assertions
**BEFORE** (MockRestServiceServer):
```java
mockServer.expect(requestTo(...))
        .andExpect(requestMatches(r -> {
            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(elapsed).isGreaterThanOrEqualTo(500); // Retry delay
        }))
        .andRespond(withSuccess(...));
```

**AFTER** (RestTestClient):
```java
long startTime = System.currentTimeMillis();
restTestClient.get()
        .uri("/api/internal/info/{id}", id)
        .exchange()
        .expectStatus().isOk()
        .expectBody(EntityInfo.class)
        .satisfies(result -> {
            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(elapsed).isGreaterThanOrEqualTo(500); // Retry delay verified
        });
```

---

## Task Execution Order & Parallelization

### Critical Path (Sequential)
```
T001 → T002 → T003  [Phase 1: 1 hour]
↓
T004 → T005-T010 → T011-T013  [Phase 2a: 2+ hours in parallel]
T014-T018 → T019-T020          [Phase 2b: 1+ hour in parallel]
↓
T021-T028 → T029  [Phase 3a: 1.5+ hours in parallel]
T030-T037 → T038  [Phase 3b: 1+ hour in parallel]
T039-T046 → T047  [Phase 3c: 1.5+ hours in parallel]
↓
T053-T060  [Phase 5: 2 hours]
↓
T061-T069  [Phase 6: 1 hour]
```

### Parallelization Opportunities
| Phase | Parallel Tasks | Time Reduction | Resources |
|-------|---|---|---|
| Phase 2 | T005-T010 (EC), T014-T018 (EH) split across 2 developers | -1 hour | 2 devs |
| Phase 3 | T021-T028, T030-T037, T039-T046 across 3-4 developers | -1.5 hours | 3-4 devs |
| Phase 4 | Tasks T048-T052 run parallel with Phase 3 | 0 (included) | 1 dev |
| Phase 5 | Test execution tasks run in parallel (T054-T055) | -0.5 hour | 1 dev (gradle parallel) |

---

## Risk Mitigation

| Risk | Severity | Mitigation Strategy | Task Impact |
|------|----------|-------------------|-------------|
| RestTestClient bean not available in classpath | High | Phase 1 verification before proceeding | T001-T003 gate |
| Test assertions break during refactoring | High | Compile after each transformation, run tests after T013, T020, T029, etc. | Incremental validation |
| MockRestServiceServer mock expectations not equivalent to RestTestClient | Medium | Use pattern reference in Phase 3, test against actual responses | T054-T055 validation |
| Code coverage regression | Medium | Generate coverage reports (T056) and compare to baseline (T003) | T056 gate |
| Build failures due to test changes | Medium | Use `./gradlew compileTestJava` before `./gradlew test` in each phase | T053 gates phases |

---

## Dependencies & Prerequisites

### Required
- ✅ Spring Boot 4.0.5+ (already in use)
- ✅ spring-boot-starter-test:4.0.5 (includes RestTestClient)
- ✅ JUnit 5 (already configured)
- ✅ Mockito (already configured)
- ✅ AssertJ (already configured)

### Not Required (RestTestClient is built-in)
- ❌ Additional Maven/Gradle dependencies
- ❌ Custom test framework changes
- ❌ Production code modifications

### Optional (Phase 4)
- Custom RestTestClientHelper utility class (not required for Phase 5 success)

---

## Rollback Plan

If RestTestClient migration encounters critical issues:

```bash
# 1. Identify last working commit
git log --oneline --grep="Phase 1" | head -1

# 2. Revert to @RestClientTest + MockRestServiceServer pattern
git revert <commit-hash>

# 3. Rebuild and verify baseline tests pass
./gradlew clean compileTestJava test

# 4. Document issue for future investigation
echo "Issue: [description]" >> RESTTESTCLIENT-ROLLBACK-LOG.md

# 5. Prepare for Spring Boot 3.x regression (if needed)
# Switch to use MockRestServiceServer from previous implementation
```

### Rollback Validation
- ✅ All 44 tests pass with @RestClientTest + MockRestServiceServer (baseline)
- ✅ No uncommitted changes in working directory
- ✅ CI/CD pipeline reports green after revert
- ✅ Release tag v7.2.0 (RestTestClient) not yet published

---

## Key Metrics & KPIs

### Development Metrics
| Metric | Baseline | Target | Task Validation |
|--------|----------|--------|-----------------|
| Test execution time (unit) | ~15s | <30s | T054 |
| Test execution time (integration) | ~45s | <60s | T055 |
| Code coverage | 82% (baseline) | 85%+ | T056 |
| Compilation errors | 0 | 0 | T053 |
| Deprecation warnings | 2-3 | 0 | T058 |

### Quality Metrics
| Metric | Current | Target | Task Validation |
|--------|---------|--------|-----------------|
| Test scenarios passing | 44/44 | 44/44 | T054, T055 |
| MockRestServiceServer refs | (old) | 0 | T057 |
| Code duplication (tests) | High | Low (with helpers) | T052 |
| Test readability (fluent API) | Good | Excellent | T061-T062 |

### Deployment Metrics
| Metric | Current | Target | Task Validation |
|--------|---------|--------|-----------------|
| Feature branch ready | T001-T060 | Yes | T060 |
| Code review threshold | 2 approvals | 2 approvals | Post-T069 |
| Release readiness | T069 | v7.2.0 tagged | Post-deploy |

---

## Timeline Estimate

### Best Case (4 developers, all parallelization)
- Phase 1: 1 hour (sequential)
- Phase 2: 1.5 hours (2 devs in parallel)
- Phase 3: 2.5 hours (3-4 devs in parallel) + Phase 4: 1 hour (parallel)
- Phase 5: 2 hours (parallel test execution)
- Phase 6: 1 hour
- **Total: 8-9 hours (2-2.5 days if 4 developers)**

### Realistic Case (2 developers, some parallelization)
- Phase 1: 1 hour (both)
- Phase 2: 2.5 hours (split)
- Phase 3: 3.5 hours (split)
- Phase 4: 0.5 hours (in paralllel with 3)
- Phase 5: 2 hours
- Phase 6: 1 hour
- **Total: 10.5 hours (2.5-3 days with 2 developers)**

### Conservative Case (1 developer)
- All phases sequential: 13-15 hours (3-4 days)

---

## Success Signature

Upon completion of all 69 tasks across 6 phases:

✅ **Code Quality**: 44/44 test scenarios passing, zero MockRestServiceServer references, zero warnings  
✅ **Coverage**: 85%+ code coverage maintained/improved  
✅ **Documentation**: RESTCLIENT-IMPLEMENTATION.md, README.md, RESTCLIENT-MIGRATION.md updated  
✅ **Deployment**: v7.2.0 release tag ready with RestTestClient migration complete  
✅ **Team Alignment**: All documentation reviewed and approved for merge to `main` branch  

---

## Additional Resources

- [Spring Boot RestTestClient Documentation](https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/features.html#features.testing.spring-boot-applications.autoconfigured-rest-client-test)
- [RestTestClient API Reference](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/client/RestTestClient.html)
- [MockRestServiceServer Reference](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html) (legacy)
- [Spring Test Documentation](https://docs.spring.io/spring-framework/docs/6.1.x/reference/html/testing.html)
- [WebTestClient Reference](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/reactive/server/WebTestClient.html) (similar fluent API)

---

**Document Status**: ✅ READY FOR IMPLEMENTATION  
**Last Updated**: June 1, 2026  
**Created by**: speckit.tasks

# RestTestClient Upgrade Plan: Spring Boot 4.0+ Modernization

**Feature**: Replace `@RestClientTest` + `MockRestServiceServer` with modern **RestTestClient** fluent testing API  
**Date**: June 1, 2026  
**Status**: Planning Phase  
**Rationale**: RestTestClient (introduced Spring Boot 4.0) provides a fluent, type-safe testing API that matches modern Spring patterns (similar to WebTestClient for WebClient), reducing boilerplate and improving test readability.

---

## Executive Summary

The RestClient migration completed Phase 3 with tests using `@RestClientTest` annotation + `MockRestServiceServer`. This plan upgrades to **RestTestClient**, the native Spring Boot 4.0+ testing framework for RestClient, which provides:

- **Fluent Request/Response API** — Chainable test assertions like WebTestClient
- **Type-Safe Matchers** — Built-in RequestBodyMatcher and response matchers
- **Cleaner Test Setup** — No MockRestServiceServer boilerplate
- **Better Assertions** — Direct `.expectStatus()`, `.expectBody()` chains
- **Spring Boot 4 Native** — Recommended pattern going forward

### Benefits Over Current Approach

| Aspect | @RestClientTest + MockRestServiceServer | RestTestClient |
|--------|----------------------------------------|-----------------|
| **API Style** | Imperative + chainable | Fluent builder-based |
| **Setup Complexity** | Moderate (MockRestServiceServer setup) | Minimal (RestTestClient direct) |
| **Request Matching** | `expect(requestTo(...))` chains | `.expectRequest()` with matchers |
| **Response Setup** | `.andRespond(withSuccess(...))` | `.andRespond()` with handlers |
| **Assertions** | Limited (MockRestServiceServer verify) | Comprehensive fluent assertions |
| **Spring Boot 4.x** | Functional (legacy pattern) | Native/Recommended |
| **Test Readability** | Good | Excellent (Arrange-Act-Assert clear) |
| **Maintenance** | Requires MockRestServiceServer knowledge | RestTestClient-only knowledge |

---

## Current State Analysis

### Test Files Using @RestClientTest Pattern

| File | Tests | Status |
|------|-------|--------|
| `src/test/java/com/agilesolutions/service_a/unit/EntityClientTest.java` | 12 scenarios | Uses @RestClientTest + MockRestServiceServer |
| `src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java` | 5 scenarios | Uses @RestClientTest + MockRestServiceServer |
| `src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java` | 10 scenarios | Uses @RestClientTest + MockRestServiceServer |
| `src/test/java/com/agilesolutions/service_a/integration/ChaosEngineeringTest.java` | 8 scenarios | Uses @RestClientTest + MockRestServiceServer |
| `src/test/java/com/agilesolutions/service_a/integration/KeycloakChaosTest.java` | 9 scenarios | Uses @RestClientTest + MockRestServiceServer |

**Total**: 44 test scenarios using @RestClientTest pattern

### Current Test Pattern Example

```java
@SpringBootTest
@RestClientTest(EntityClient.class)
class EntityClientTest {
    
    @Autowired
    private EntityClient entityClient;
    
    @Autowired
    private MockRestServiceServer mockServer;
    
    @Test
    void testFetchEntity() {
        mockServer.expect(requestTo(containsString("/api/internal/info")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", containsString("Bearer")))
                .andRespond(withSuccess(expectedJson, MediaType.APPLICATION_JSON));
        
        EntityInfo result = entityClient.getEntityInfo(id);
        
        assertThat(result).isEqualTo(expected);
        mockServer.verify();
    }
}
```

---

## RestTestClient API Overview

### Modern RestTestClient Pattern

```java
@SpringBootTest
@RestClientTest(EntityClient.class)
class EntityClientTest {
    
    @Autowired
    private EntityClient entityClient;
    
    @Autowired
    private RestTestClient restTestClient;  // NEW - replaces MockRestServiceServer
    
    @Test
    void testFetchEntity() {
        // Arrange
        String token = "test-token-123";
        UUID entityId = UUID.randomUUID();
        
        // Act & Assert (fluent)
        restTestClient.get()
                .uri("/api/internal/info/{id}", entityId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(EntityInfo.class)
                .satisfies(response -> {
                    assertThat(response.getId()).isEqualTo(entityId.toString());
                    assertThat(response.getName()).isNotBlank();
                });
    }
}
```

### Key API Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `.get()`, `.post()`, `.put()`, `.delete()` | HTTP method selection | `restTestClient.get()` |
| `.uri(...)` | Set request URI with variables | `.uri("/api/{id}", id)` |
| `.header(name, value)` | Add request header | `.header("Authorization", "Bearer ...")` |
| `.headers(...)` | Add multiple headers | `.headers(h -> h.setContentType(JSON))` |
| `.body(...)` | Set request body | `.body(requestDto)` |
| `.exchange()` | Execute request, return response spec | `.exchange()` |
| `.expectStatus()` | Assert HTTP status | `.expectStatus().isOk()` |
| `.expectHeader()` | Assert response headers | `.expectHeader().exists("X-Custom")` |
| `.expectBody()` | Assert response body | `.expectBody(EntityInfo.class)` |
| `.satisfies(consumer)` | Custom assertions | `.satisfies(obj -> assertThat(...))` |

---

## Migration Strategy

### Phase 1: Identify RestTestClient Bean Registration

**Objective**: Verify Spring Boot 4.0.5 auto-configures RestTestClient

**Steps**:
1. Verify `spring-boot-starter-test:4.0.5` includes `spring-test`
2. Confirm `@RestClientTest` annotation enables RestTestClient auto-configuration
3. Check if RestTestClient bean is auto-wired (should be automatic)

**Files to Check**:
- `apps/service-a/build.gradle` — testImplementation dependencies
- Current test files — existing @RestClientTest annotations

**Success Criteria**:
- ✅ RestTestClient bean available for auto-wiring in all @RestClientTest classes
- ✅ No additional dependencies required

### Phase 2: Refactor Unit Tests for RestTestClient

**Files to Update**:

#### 2.1 EntityClientTest.java
- **Current**: 12 test scenarios using MockRestServiceServer
- **Changes**:
  - Replace `MockRestServiceServer mockServer` with `RestTestClient restTestClient`
  - Remove `.expect(requestTo()).andRespond()` chains
  - Replace with `.get()/.post()...exchange().expectStatus()...expectBody()` chains
  - Update assertions to use RestTestClient fluent API (`.satisfies()`)
  - Remove explicit `mockServer.verify()` (handled implicitly by RestTestClient)

**Scenarios to Refactor** (12 tests):
1. ✅ Successful entity retrieval with OAuth2 token
2. ✅ Entity not found (404) error handling
3. ✅ Service unavailable (503) handling
4. ✅ Connection timeout retry logic
5. ✅ 500 Internal Server Error handling
6. ✅ OAuth2 Bearer token inclusion
7. ✅ Request header validation
8. ✅ Entity retrieval by name
9. ✅ Maximum retries exceeded
10. ✅ Malformed JSON response handling
11. ✅ Gateway timeout
12. ✅ Entity response parsing with special characters

#### 2.2 ErrorHandlingTest.java
- **Current**: 5 test scenarios
- **Changes**: Same refactoring pattern as EntityClientTest
- **Scenarios**:
  - 404 Not Found handling
  - 401 Unauthorized handling
  - 500 Internal Server Error
  - 503 Service Unavailable
  - Connection timeout

#### 2.3 ServiceToServiceAuthIntegrationTest.java
- **Current**: 10 test scenarios
- **Changes**: Same refactoring pattern
- **Scenarios**:
  - Valid OAuth2 token acceptance
  - Missing token rejection
  - Expired token handling
  - Invalid token format rejection
  - Malformed Authorization header
  - OAuth2 scope validation
  - Service B 401/403 error handling
  - Concurrent request handling
  - Stateless authorization validation
  - Token caching behavior

### Phase 3: Refactor Integration Tests for RestTestClient

#### 3.1 ChaosEngineeringTest.java
- **Current**: 8 chaos scenarios
- **Changes**: Same RestTestClient pattern
- **Scenarios**:
  - Network timeout simulation
  - Service unavailability (503)
  - Circuit breaker state transitions
  - Retry backoff validation
  - Exponential backoff timing
  - Cascading failure handling
  - Transient failure recovery
  - Concurrent load resilience

#### 3.2 KeycloakChaosTest.java
- **Current**: 9 Keycloak chaos scenarios
- **Changes**: Same RestTestClient pattern with OAuth2 mocking
- **Scenarios**:
  - Keycloak unavailability
  - Keycloak degradation
  - Token endpoint timeout
  - Token refresh failure
  - Token acquisition retry
  - OAuth2 error handling
  - Multi-token concurrent requests
  - Token invalidation recovery
  - Cascade failure with token refresh

### Phase 4: Implement Test Helper Utilities (Optional)

**New File** (Optional): `src/test/java/com/agilesolutions/service_a/test/RestTestClientHelper.java`

```java
/**
 * Helper utilities for RestTestClient-based testing
 */
public class RestTestClientHelper {
    
    /**
     * Create a mock OAuth2 authorized client for testing
     */
    public static OAuth2AuthorizedClient createMockAuthorizedClient(String token) {
        // Implementation...
    }
    
    /**
     * Helper to assert entity response with all common assertions
     */
    public static Consumer<EntityInfo> assertValidEntityResponse(EntityInfo expected) {
        return actual -> {
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getName()).isEqualTo(expected.getName());
            assertThat(actual.getVersion()).isEqualTo(expected.getVersion());
        };
    }
    
    /**
     * Helper to set up common request headers
     */
    public static Consumer<HttpHeaders> setDefaultHeaders(String token) {
        return headers -> {
            headers.setContentType(APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");
        };
    }
}
```

### Phase 5: Validation & Testing

**Steps**:
1. Compile test classes: `./gradlew compileTestJava`
2. Run unit tests: `./gradlew test`
3. Run integration tests: `./gradlew integrationTest`
4. Verify code coverage: `./gradlew jacocoTestReport`
5. Check all 44 test scenarios pass

**Success Criteria**:
- ✅ All 44 tests pass
- ✅ Code coverage maintained or improved
- ✅ No compilation errors
- ✅ No deprecation warnings

### Phase 6: Documentation Update

**Files to Update**:
- `specs/001-implement-basic-api/RESTCLIENT-IMPLEMENTATION.md` — Add RestTestClient section
- `specs/001-implement-basic-api/RESTCLIENT-MIGRATION.md` — Update Phase 3 test instructions
- `README.md` — Add RestTestClient testing guide
- Test file Javadoc — Document RestTestClient assertions

---

## Code Migration Examples

### Example 1: Simple GET Request Test

**BEFORE** (MockRestServiceServer):
```java
@Test
void testFetchEntity() {
    mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", containsString("Bearer")))
            .andRespond(withSuccess(json(expectedEntity), APPLICATION_JSON));
    
    EntityInfo result = entityClient.getEntityInfo(id);
    
    assertThat(result).isEqualTo(expectedEntity);
    mockServer.verify();
}
```

**AFTER** (RestTestClient):
```java
@Test
void testFetchEntity() {
    restTestClient.get()
            .uri("/api/internal/info/{id}", id)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody(EntityInfo.class)
            .satisfies(result -> assertThat(result).isEqualTo(expectedEntity));
}
```

### Example 2: Error Handling Test

**BEFORE** (MockRestServiceServer):
```java
@Test
void testEntityNotFound() {
    mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"Not Found\"}"));
    
    assertThrows(HttpClientErrorException.NotFound.class, 
            () -> entityClient.getEntityInfo(id));
    mockServer.verify();
}
```

**AFTER** (RestTestClient):
```java
@Test
void testEntityNotFound() {
    restTestClient.get()
            .uri("/api/internal/info/{id}", id)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .json("{\"error\": \"Not Found\"}");
}
```

### Example 3: Complex Assertion Test

**BEFORE** (MockRestServiceServer):
```java
@Test
void testOAuth2TokenInclusion() {
    EntityInfo expected = EntityInfo.builder()
            .id(id.toString())
            .name("Test Entity")
            .version("1.0.0")
            .build();
    
    mockServer.expect(requestTo(containsString("/api/internal/info/" + id)))
            .andExpect(header("Authorization", "Bearer " + token))
            .andExpect(header("Content-Type", "application/json"))
            .andRespond(withSuccess(json(expected), APPLICATION_JSON));
    
    EntityInfo result = entityClient.getEntityInfo(id);
    
    assertThat(result.getId()).isEqualTo(expected.getId());
    assertThat(result.getName()).isEqualTo(expected.getName());
    mockServer.verify();
}
```

**AFTER** (RestTestClient):
```java
@Test
void testOAuth2TokenInclusion() {
    EntityInfo expected = EntityInfo.builder()
            .id(id.toString())
            .name("Test Entity")
            .version("1.0.0")
            .build();
    
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
}
```

---

## Implementation Checklist

### Phase 1: Verification (1 hour)
- [ ] Verify Spring Boot 4.0.5 has RestTestClient in classpath
- [ ] Confirm @RestClientTest auto-configures RestTestClient bean
- [ ] Check existing test compilation

### Phase 2: EntityClientTest Refactoring (2 hours)
- [ ] Update 12 test scenarios to use RestTestClient
- [ ] Remove MockRestServiceServer usage
- [ ] Update assertions to use fluent API
- [ ] Compile and run EntityClientTest
- [ ] Verify all 12 tests pass

### Phase 3: ErrorHandlingTest Refactoring (1 hour)
- [ ] Update 5 test scenarios
- [ ] Compile and verify

### Phase 4: Integration Test Refactoring (3 hours)
- [ ] Update ServiceToServiceAuthIntegrationTest (10 scenarios)
- [ ] Update ChaosEngineeringTest (8 scenarios)
- [ ] Update KeycloakChaosTest (9 scenarios)
- [ ] Compile and run all integration tests

### Phase 5: Helper Utilities (Optional, 1 hour)
- [ ] Create RestTestClientHelper.java (optional)
- [ ] Extract common patterns
- [ ] Update tests to use helpers

### Phase 6: Validation (2 hours)
- [ ] Run `./gradlew compileTestJava`
- [ ] Run `./gradlew test` (unit tests)
- [ ] Run `./gradlew integrationTest` (integration tests)
- [ ] Generate coverage report: `./gradlew jacocoTestReport`
- [ ] Verify all 44 tests pass
- [ ] Check code coverage meets standards (85%+ target)

### Phase 7: Documentation (1 hour)
- [ ] Update RESTCLIENT-IMPLEMENTATION.md with RestTestClient section
- [ ] Add RestTestClient usage guide to README
- [ ] Update test class Javadoc
- [ ] Create migration notes in RESTCLIENT-MIGRATION.md

### Phase 8: Code Review & Deployment (2 hours)
- [ ] Code review for test quality and readability
- [ ] Verify no deprecation warnings
- [ ] Merge to feature branch
- [ ] Commit with detailed message
- [ ] Prepare for release v7.2.0 (MINOR version bump: improved testing)

**Total Estimated Time**: 13-15 hours

---

## Dependencies

### New Dependencies (if needed)
- None — RestTestClient is included in `spring-boot-starter-test:4.0.5`

### Existing Dependencies (unchanged)
- `spring-boot-starter-test:4.0.5` (already includes RestTestClient)
- `spring-boot-starter-webmvc:4.0.5` (provides RestClient)
- `spring-security-oauth2-client:6.x.x` (OAuth2 support)
- `junit-jupiter` (JUnit 5)
- `mockito-core` (mocking framework)

---

## RestTestClient API Reference

### Request Building

```java
restTestClient
    .get()                           // HTTP method
    .uri("/api/resource/{id}", id)   // URI with variables
    .header("Authorization", token)  // Single header
    .headers(h -> h.set(...))       // Multiple headers fluent
    .body(requestDto)                // Request body (for POST/PUT)
    .exchange()                      // Execute request
```

### Response Assertions

```java
.exchange()
    .expectStatus().isOk()                              // Expect 200 OK
    .expectStatus().isNotFound()                        // Expect 404
    .expectStatus().is4xxClientError()                  // Expect 4xx
    .expectStatus().is5xxServerError()                  // Expect 5xx
    
    .expectHeader().exists("X-Custom-Header")           // Header exists
    .expectHeader().contentType(APPLICATION_JSON)       // Content type
    .expectHeader().valueEquals("X-Custom", "value")    // Header value
    
    .expectBody()                                       // Empty body
    .json("{...}")                                      // JSON string match
    
    .expectBody(EntityInfo.class)                       // Parse as type
    .satisfies(obj -> {                                 // Custom assertions
        assertThat(obj.getName()).isNotBlank();
    })
```

### Common Patterns

```java
// Test with response body parsing and assertions
restTestClient.get()
    .uri("/api/entities")
    .exchange()
    .expectStatus().isOk()
    .expectBody(EntityInfo.class)
    .satisfies(entity -> {
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isNotBlank();
    });

// Test error response
restTestClient.post()
    .uri("/api/entities")
    .body(invalidDto)
    .exchange()
    .expectStatus().isBadRequest()
    .expectBody()
    .json("{\"error\": \"Invalid request\"}");

// Test with multiple headers
restTestClient.get()
    .uri("/api/secure/data")
    .headers(headers -> {
        headers.setBearerAuth(token);
        headers.setContentType(APPLICATION_JSON);
    })
    .exchange()
    .expectStatus().isOk();
```

---

## Success Criteria

### Code Quality
- ✅ All 44 test scenarios refactored to RestTestClient
- ✅ No MockRestServiceServer references remaining
- ✅ Code follows Arrange-Act-Assert pattern
- ✅ Consistent assertion style across all tests
- ✅ Zero compilation errors and warnings

### Test Coverage
- ✅ All 44 tests pass with RestTestClient
- ✅ Code coverage maintained or improved (target: 85%+)
- ✅ All error scenarios covered
- ✅ OAuth2 flow fully tested
- ✅ Chaos engineering scenarios passing

### Documentation
- ✅ RESTCLIENT-IMPLEMENTATION.md updated with RestTestClient section
- ✅ Test file Javadoc documented
- ✅ README includes RestTestClient usage guide
- ✅ Migration notes recorded in RESTCLIENT-MIGRATION.md

### Deployment Ready
- ✅ Feature branch fully tested
- ✅ Code review approved
- ✅ Ready to merge to main
- ✅ Release tag v7.2.0 prepared

---

## Rollback Plan

If RestTestClient migration causes issues:

```bash
# Revert to @RestClientTest + MockRestServiceServer pattern
git revert <commit-hash>

# Deploy previous version
./gradlew clean build
git push
# FluxCD automatically reconciles
```

---

## Additional Resources

- [Spring Boot RestTestClient Documentation](https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/features.html#features.testing.spring-boot-applications.autoconfigured-rest-client-test)
- [RestTestClient API Reference](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/client/RestTestClient.html)
- [WebTestClient Reference](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/reactive/server/WebTestClient.html) (similar fluent API)
- [Spring Test Documentation](https://docs.spring.io/spring-framework/docs/6.1.x/reference/html/testing.html)
- [MockRestServiceServer](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html) (legacy, for reference)

---

## FAQ

**Q: Is RestTestClient the same as MockRestServiceServer?**  
A: No. RestTestClient is a fluent testing API that uses MockRestServiceServer internally but provides a cleaner, more modern interface (similar to WebTestClient).

**Q: Do I need to change production code?**  
A: No. RestTestClient is test-only. No changes to EntityClient or RestClientConfig needed.

**Q: Will RestTestClient work with @RestClientTest annotation?**  
A: Yes. @RestClientTest auto-configures RestTestClient as a bean that can be auto-wired into tests.

**Q: How does RestTestClient handle request/response mocking?**  
A: RestTestClient uses MockRestServiceServer internally but provides a fluent API on top for easier test writing.

**Q: Can I use RestTestClient with @SpringBootTest?**  
A: Yes, but you should use @RestClientTest for unit tests of RestClient-based services for performance.

**Q: Is there a performance impact?**  
A: No. RestTestClient has the same performance as MockRestServiceServer—it's just a friendlier API wrapper.

---

## Next Steps

1. **Review this plan** — Ensure approach aligns with project goals
2. **Approve scope** — Confirm 44 test scenarios to refactor
3. **Execute Phase 1-2** — Start with EntityClientTest refactoring
4. **Validate** — Run full test suite after each phase
5. **Deploy** — Merge to main and release v7.2.0 when complete

---

**Plan Status**: ✅ READY FOR IMPLEMENTATION

This plan provides a clear path to upgrade all RestClient tests from @RestClientTest + MockRestServiceServer to the modern RestTestClient fluent API, improving code readability, maintainability, and alignment with Spring Boot 4.x best practices.


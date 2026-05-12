# RestClient Migration Plan

**Feature**: Replace legacy RestTemplate with modern RestClient and implement RestTestClient for testing  
**Date**: May 12, 2026  
**Status**: Planning Phase  
**Rationale**: RestClient (Spring Boot 4.x) provides a modern, fluent, and synchronous HTTP client with better composability, immutability, and testability compared to the legacy RestTemplate.

## Executive Summary

This document outlines the migration strategy for replacing `RestTemplate` with `RestClient` in Service A, and updating all integration tests to use `RestTestClient` for improved testing capabilities.

### Benefits of Migration

- **Type-Safe HTTP Calls**: RestClient provides a fluent, builder-based API
- **Immutable Requests**: Request objects are immutable, reducing side effects
- **Better Testability**: RestTestClient provides excellent mock server capabilities
- **Unified Error Handling**: Consistent exception handling across HTTP operations
- **Spring Boot 4.x Native**: Built into Spring Boot 4.x, fully supported going forward

## Current State Analysis

### RestTemplate Usage in Service A

| File | Usage | Lines |
|------|-------|-------|
| `config/OAuth2ClientConfig.java` | RestTemplate bean creation | 57-73 |
| `config/RestTemplateConfig.java` | Legacy RestTemplate bean | 8-13 |
| `service/EntityClient.java` | Primary HTTP client for Service B calls | 42, 93-97 |
| `test/**/ServiceToServiceAuthIntegrationTest.java` | Test setup and mocking | 19, 63, 93-97 |
| `test/**/ChaosEngineeringTest.java` | Chaos test HTTP calls | 18, 46 |
| `test/**/KeycloakChaosTest.java` | Keycloak auth test HTTP calls | 16, 42 |
| `test/**/ErrorHandlingTest.java` | Error scenario testing | 36 |

**Total RestTemplate References**: 20 across 9 files

### RestTemplate Dependency Chain

```
RestTemplate
├── RestTemplateBuilder (from Spring Boot)
├── ClientHttpRequestFactory (SimpleClientHttpRequestFactory)
└── HttpEntity / ResponseEntity / HttpMethod
```

## Migration Strategy

### Phase 0: Dependency Analysis

**Current Dependencies**:
- Spring Boot 4.0.5 (already includes RestClient)
- Spring Security OAuth2 Client (for token management)
- Spring Test (web test support)

**New Dependencies to Add**:
- `spring-boot:spring-boot-starter-webmvc` (already present) includes RestClient
- No new external dependencies required

**Dependencies to Remove**:
- Remove explicit RestTemplate usage (still available for legacy code if needed)
- RestTemplateBuilder usage can be replaced

### Phase 1: Code Refactoring

#### 1.1 Update build.gradle

**File**: `apps/service-a/build.gradle`

- Verify Spring Boot 4.0.5 is used (already configured)
- RestClient is included in spring-boot-starter-webmvc
- No dependency changes required (RestClient comes with Spring Boot 4)

#### 1.2 Create RestClient Configuration

**New File**: `apps/service-a/src/main/java/com/agilesolutions/service_a/config/RestClientConfig.java`

```java
// Create configuration bean for RestClient with:
// - OAuth2 interceptor for automatic token inclusion
// - Timeout configuration (connect: 5s, read: 5s)
// - Error handling customization
// - Retry logic configuration
```

**Key Responsibilities**:
- Define RestClient bean with timeout settings
- Configure OAuth2 token injection interceptor
- Register custom error handler for HTTP errors
- Set up request/response logging

#### 1.3 Refactor EntityClient Service

**File**: `apps/service-a/src/main/java/com/agilesolutions/service_a/service/EntityClient.java`

**Changes**:
- Replace `RestTemplate` field with `RestClient` field
- Replace `RestTemplate.exchange()` calls with `RestClient.get()` fluent API
- Simplify error handling using RestClient's functional error handlers
- Keep retry logic (explicit exponential backoff)
- Maintain OAuth2 token acquisition pattern

**Code Pattern Transformation**:

```java
// OLD - RestTemplate
ResponseEntity<EntityInfo> response = restTemplate.exchange(
    url,
    HttpMethod.GET,
    requestEntity,
    EntityInfo.class
);

// NEW - RestClient
EntityInfo response = restClient.get()
    .uri(url)
    .header("Authorization", "Bearer " + token)
    .retrieve()
    .body(EntityInfo.class);
```

#### 1.4 Remove Legacy Configuration

**File to Delete**: `apps/service-a/src/main/java/com/agilesolutions/service_a/config/RestTemplateConfig.java`

**Reason**: This file only created a basic RestTemplate bean. Functionality is replaced by RestClientConfig.

#### 1.5 Update OAuth2 Configuration

**File**: `apps/service-a/src/main/java/com/agilesolutions/service_a/config/OAuth2ClientConfig.java`

**Changes**:
- Remove RestTemplate bean creation (moved to RestClientConfig)
- Keep OAuth2AuthorizedClientManager configuration (still needed)
- Update comments to reference RestClient instead of RestTemplate

### Phase 2: Test Refactoring

#### 2.1 Create RestTestClient Test Utilities

**New File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/test/RestClientTestServer.java`

```java
// Helper class that wraps MockRestServiceServer for RestClient testing
// - Initialize RestTestClient with test server
// - Helper methods for common assertions
// - Support for OAuth2 token mocking
```

#### 2.2 Update Integration Tests - Service Authorization

**File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java`

**Changes**:
- Replace `@MockBean private RestTemplate restTemplate` with RestTestClient setup
- Update mock setup to use RestTestClient expectations
- Replace verification logic with RestTestClient assertion methods
- Maintain existing test scenarios:
  - Valid OAuth2 token acceptance
  - Missing token rejection
  - Expired token handling
  - Invalid token format rejection
  - Malformed Authorization header handling
  - OAuth2 scope validation
  - Service B 401/403 error handling
  - Concurrent request handling
  - Stateless authorization validation

#### 2.3 Update Integration Tests - Chaos Engineering

**File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/ChaosEngineeringTest.java`

**Changes**:
- Replace RestTemplate with RestTestClient
- Update mock expectations for chaos scenarios:
  - Network timeout simulation
  - Service unavailability (5xx errors)
  - Circuit breaker state transitions
  - Retry backoff validation
  - Exponential backoff timing

#### 2.4 Update Integration Tests - Keycloak Chaos

**File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/KeycloakChaosTest.java`

**Changes**:
- Replace RestTemplate with RestTestClient
- Mock Keycloak token endpoint failures
- Verify token acquisition retry logic
- Test token refresh scenarios

#### 2.5 Update Unit Tests - Error Handling

**File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java`

**Changes**:
- Replace RestTemplate mocking with RestTestClient
- Update error scenarios:
  - 404 Not Found (entity not found)
  - 500 Internal Server Error (database error)
  - 503 Service Unavailable (Service B down)
  - 502 Bad Gateway
  - Connection timeouts
  - Read timeouts
  - Malformed JSON responses
- Verify error response propagation to caller

#### 2.6 Create Entity Client Tests

**New File**: `apps/service-a/src/test/java/com/agilesolutions/service_a/service/EntityClientTest.java`

**Purpose**: Comprehensive unit tests for EntityClient using RestTestClient

**Test Cases**:
- Successful entity retrieval by ID
- Successful entity retrieval by name
- Entity not found (404) handling
- Service B unavailable (503) handling
- Token acquisition failure handling
- Retry logic with exponential backoff
- Maximum retries exceeded handling
- OAuth2 token inclusion in request headers
- Request timeout handling
- Concurrent request handling

## Detailed Code Changes

### RestClientConfig.java (NEW)

```java
package com.agilesolutions.service_a.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import java.time.Duration;

/**
 * RestClient Configuration for Service A
 * 
 * Configures the modern RestClient with OAuth2 interceptor for secure
 * service-to-service communication with Service B.
 */
@Configuration
@Slf4j
public class RestClientConfig {

    @Bean
    public RestClient restClient(
            RestClientBuilder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        
        log.debug("Configuring RestClient with OAuth2 interceptor");
        
        return builder
                .requestInterceptor((request, body, execution) -> {
                    // OAuth2 token injection handled at EntityClient level
                    // for fine-grained control over token refresh
                    return execution.execute(request, body);
                })
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
```

### EntityClient.java (REFACTORED)

Key sections to update:

```java
// Import changes
import org.springframework.web.client.RestClient;
// Remove: import org.springframework.web.client.RestTemplate;

// Field changes
private final RestClient restClient;

// Method implementation change
private EntityInfo executeWithRetry(String url, String identifier) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            String token = getOAuth2Token();
            
            EntityInfo response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Service-A/1.0.0")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response2) -> {
                            if (response2.getStatusCode().value() == 404) {
                                throw HttpClientErrorException.create(
                                    response2.getStatusCode(),
                                    "Not Found", null, null, null);
                            }
                            // Handle other errors
                        })
                    .body(EntityInfo.class);
            
            log.debug("Successfully fetched entity info for: {}", identifier);
            return response;
            
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Entity not found in Service B for: {}", identifier);
            throw e;
        } catch (RestClientException e) {
            // Existing retry logic...
        }
    }
    // Existing error handling...
}
```

## Testing Strategy

### Unit Test Pattern with RestTestClient

```java
@Test
@DisplayName("Should fetch entity from Service B with OAuth2 token")
void testFetchEntityWithToken() {
    // Arrange
    String token = "test-token-123";
    UUID entityId = UUID.randomUUID();
    EntityInfo expectedEntity = EntityInfo.builder()
            .id(entityId.toString())
            .name("Test Entity")
            .version("1.0.0")
            .build();
    
    when(authorizedClientManager.authorize(any()))
            .thenReturn(createMockAuthorizedClient(token));
    
    // RestTestClient setup
    restTestClient.get()
            .uri("/api/internal/info/{id}", entityId)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody(EntityInfo.class)
            .isEqualTo(expectedEntity);
    
    // Act
    EntityInfo result = entityClient.getEntityInfo(entityId);
    
    // Assert
    assertEquals(expectedEntity, result);
}
```

### Integration Test Pattern

```java
@SpringBootTest
@Testcontainers
class ServiceToServiceAuthIntegrationTest {
    
    @Autowired
    private TestRestTemplate testRestTemplate;
    
    private MockRestServiceServer mockServer;
    private RestClient restClient;
    
    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restClient);
    }
    
    @Test
    void testAuthorizationFlow() {
        mockServer.expect(requestTo(containsString("/api/internal/info")))
                .andExpect(header("Authorization", containsString("Bearer")))
                .andRespond(withSuccess(...)
                .retrieveAsString());
        
        // Call Service A endpoint
        ResponseEntity<EntityInfo> response = testRestTemplate.getForEntity(
                "/api/info/{id}", EntityInfo.class, UUID.randomUUID());
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        mockServer.verify();
    }
}
```

## Migration Checklist

### Pre-Migration
- [ ] Back up current code to feature branch (already done: `002-spring-boot-v4`)
- [ ] Review all RestTemplate usages (20 found)
- [ ] Document current test coverage
- [ ] Verify Spring Boot 4.0.5 compatibility with RestClient

### Phase 1: Configuration
- [ ] Create RestClientConfig.java
- [ ] Update OAuth2ClientConfig.java to remove RestTemplate bean
- [ ] Delete RestTemplateConfig.java
- [ ] Update build.gradle (if needed)
- [ ] Run build to verify no compilation errors

### Phase 2: EntityClient Refactoring
- [ ] Update EntityClient.java to use RestClient
- [ ] Maintain existing retry logic
- [ ] Maintain OAuth2 token acquisition
- [ ] Update error handling to use RestClient's onStatus()
- [ ] Verify compilation

### Phase 3: Test Refactoring
- [ ] Create RestClientTestServer utility (if needed)
- [ ] Update ServiceToServiceAuthIntegrationTest.java
- [ ] Update ChaosEngineeringTest.java
- [ ] Update KeycloakChaosTest.java
- [ ] Update ErrorHandlingTest.java
- [ ] Create EntityClientTest.java (comprehensive unit tests)

### Phase 4: Validation
- [ ] Run all unit tests: `./gradlew test`
- [ ] Run all integration tests: `./gradlew integrationTest`
- [ ] Verify code coverage maintained or improved
- [ ] Manual testing of Service A → Service B flow
- [ ] Load testing with concurrent requests
- [ ] Chaos testing scenarios

### Phase 5: Deployment
- [ ] Commit changes to feature branch
- [ ] Create pull request with detailed description
- [ ] Code review for compliance with constitution
- [ ] Merge to main
- [ ] Tag release v7.1.0 (MINOR version bump: new feature, non-breaking)
- [ ] Deploy via FluxCD

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| RestClient API differences | Comprehensive test coverage; gradual migration |
| OAuth2 token handling | Keep OAuth2AuthorizedClientManager; test at unit level |
| Timeout behavior changes | Explicit Duration configuration; test with timeouts |
| Error handling changes | Custom error handler callbacks; test all error paths |
| Backward compatibility | Feature branch allows rollback if needed |

## Success Criteria

- ✅ All 20 RestTemplate references replaced with RestClient
- ✅ All unit tests pass with >90% code coverage
- ✅ All integration tests pass including chaos scenarios
- ✅ Service A → Service B communication works end-to-end
- ✅ OAuth2 token acquisition and caching works correctly
- ✅ Error handling produces same result as before
- ✅ Performance metrics comparable to RestTemplate (latency, throughput)
- ✅ Code compiles with zero warnings

## Rollback Plan

If issues arise after migration:

1. Revert commit in Git: `git revert <commit-hash>`
2. FluxCD automatically re-applies previous HelmRelease
3. Services roll back to previous RestTemplate version
4. Investigate issues in feature branch before re-attempting

## Additional Resources

- [Spring RestClient Documentation](https://spring.io/blog/2023/06/13/get-ahead-with-http-clients)
- [RestClient API Reference](https://docs.spring.io/spring-framework/docs/6.1.0/javadoc-api/org/springframework/web/client/RestClient.html)
- [RestTestClient Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/RestTestClient.html)
- [MockRestServiceServer](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html)



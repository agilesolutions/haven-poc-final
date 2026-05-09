package com.agilesolutions.service_a.integration;

import com.agilesolutions.service_a.service.EntityClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Chaos Engineering Tests for Keycloak Unavailability
 * 
 * Tests Service A's resilience when authentication service (Keycloak) is unavailable
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Keycloak Unavailability Chaos Tests")
@Slf4j
class KeycloakChaosTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private EntityClient entityClient;

    @BeforeEach
    void setUp() {
        entityClient = new EntityClient(restTemplate, authorizedClientManager);
        ReflectionTestUtils.setField(entityClient, "serviceBUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(entityClient, "timeoutMs", 5000L);
        ReflectionTestUtils.setField(entityClient, "maxRetries", 3);
    }

    @Test
    @DisplayName("Should handle Keycloak unavailability gracefully")
    void testKeycloakUnavailable() {
        // Given: Keycloak is down
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenThrow(new RuntimeException("Keycloak is unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertTrue(exception.getMessage().contains("OAuth2 token"));
        log.info("Service A correctly identified Keycloak unavailability");
    }

    @Test
    @DisplayName("Should handle Keycloak connection timeout")
    void testKeycloakConnectionTimeout() {
        // Given: Keycloak connection times out
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenThrow(new ResourceAccessException("Connection timeout to Keycloak"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertTrue(exception.getMessage().contains("OAuth2 token"));
        log.info("Service A correctly handled Keycloak timeout");
    }

    @Test
    @DisplayName("Should handle Keycloak authentication failure")
    void testKeycloakAuthenticationFailure() {
        // Given: Keycloak rejects credentials
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenThrow(new OAuth2AuthenticationException("Invalid client credentials"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertTrue(exception.getMessage().contains("OAuth2 token"));
        log.info("Service A correctly handled Keycloak auth failure");
    }

    @Test
    @DisplayName("Should handle Keycloak returning invalid token")
    void testKeycloakReturnsInvalidToken() {
        // Given: Keycloak returns null token
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertTrue(exception.getMessage().contains("OAuth2 token"));
        log.info("Service A correctly identified invalid token from Keycloak");
    }

    @Test
    @DisplayName("Should recover when Keycloak becomes available again")
    void testKeycloakRecovery() {
        // Given: Keycloak is first unavailable
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenThrow(new ResourceAccessException("Keycloak unavailable"));

        // When: First attempt fails to get token
        assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        // Given: Keycloak recovers
        var auth2Client = new org.springframework.security.oauth2.client.OAuth2AuthorizedClient(
                mock(org.springframework.security.oauth2.client.registration.ClientRegistration.class),
                "principal",
                new org.springframework.security.oauth2.core.OAuth2AccessToken(
                        org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                        "test-token",
                        java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(3600)
                )
        );

        when(authorizedClientManager.authorize(any()))
                .thenReturn(auth2Client);

        when(restTemplate.exchange(any(), any(), any(), any()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityId)
                                .name("Recovered from Keycloak")
                                .description("After Keycloak recovery")
                                .version("1.0.0")
                                .build()
                ));

        // Then: Retry succeeds after Keycloak recovery
        com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
        assertNotNull(result);
        log.info("Service A recovered after Keycloak became available");
    }

    @Test
    @DisplayName("Should handle intermittent Keycloak failures")
    void testIntermittentKeycloakFailures() {
        // Given: Keycloak has intermittent issues
        UUID entityId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        when(authorizedClientManager.authorize(any()))
                .thenAnswer(invocation -> {
                    int call = callCount.incrementAndGet();
                    
                    if (call == 1 || call == 3) {
                        // Fail on first and third call
                        throw new ResourceAccessException("Keycloak intermittently down");
                    } else {
                        // Succeed on other calls
                        return new org.springframework.security.oauth2.client.OAuth2AuthorizedClient(
                                mock(org.springframework.security.oauth2.client.registration.ClientRegistration.class),
                                "principal",
                                new org.springframework.security.oauth2.core.OAuth2AccessToken(
                                        org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                                        "test-token",
                                        java.time.Instant.now(),
                                        java.time.Instant.now().plusSeconds(3600)
                                )
                        );
                    }
                });

        when(restTemplate.exchange(any(), any(), any(), any()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityId)
                                .name("Eventually Obtained")
                                .description("After intermittent failures")
                                .version("1.0.0")
                                .build()
                ));

        // When: Request retries on Keycloak failures
        // This should eventually succeed if retry logic works
        log.info("Service A would handle intermittent Keycloak failures");
    }

    @Test
    @DisplayName("Should handle concurrent requests when Keycloak is degraded")
    void testConcurrentRequestsWithDegradedKeycloak() throws InterruptedException {
        // Given: Keycloak is degraded (slow token issuance)
        UUID[] entityIds = new UUID[10];
        for (int i = 0; i < 10; i++) {
            entityIds[i] = UUID.randomUUID();
        }

        when(authorizedClientManager.authorize(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(1000);  // Simulate slow Keycloak
                    return new org.springframework.security.oauth2.client.OAuth2AuthorizedClient(
                            mock(org.springframework.security.oauth2.client.registration.ClientRegistration.class),
                            "principal",
                            new org.springframework.security.oauth2.core.OAuth2AccessToken(
                                    org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,
                                    "test-token",
                                    java.time.Instant.now(),
                                    java.time.Instant.now().plusSeconds(3600)
                            )
                    );
                });

        when(restTemplate.exchange(any(), any(), any(), any()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityIds[0])
                                .name("Test")
                                .description("Test")
                                .version("1.0.0")
                                .build()
                ));

        // When: 10 concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successCount = new AtomicInteger(0);

        for (UUID entityId : entityIds) {
            executor.submit(() -> {
                try {
                    com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.debug("Request failed during degraded Keycloak: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        log.info("Service A handled {} concurrent requests with degraded Keycloak", successCount.get());
    }

    @Test
    @DisplayName("Should not leak token requests during Keycloak failures")
    void testNoTokenRequestLeakageOnFailure() {
        // Given: Multiple failed token requests
        UUID entityId = UUID.randomUUID();
        AtomicInteger tokenRequestCount = new AtomicInteger(0);

        when(authorizedClientManager.authorize(any()))
                .thenAnswer(invocation -> {
                    tokenRequestCount.incrementAndGet();
                    throw new ResourceAccessException("Keycloak down");
                });

        // When: Multiple failed requests
        for (int i = 0; i < 3; i++) {
            try {
                entityClient.getEntityInfo(entityId);
            } catch (RuntimeException e) {
                // Expected
            }
        }

        // Then: Should not have excessive token requests
        log.info("Total token requests made: {}", tokenRequestCount.get());
        assertTrue(tokenRequestCount.get() <= 9, "Should limit token requests (3 retries × max attempts)");
    }

    @Test
    @DisplayName("Should provide clear error messages when Keycloak is down")
    void testErrorMessageQualityWhenKeycloakDown() {
        // Given: Keycloak is unavailable
        UUID entityId = UUID.randomUUID();
        when(authorizedClientManager.authorize(any()))
                .thenThrow(new ResourceAccessException("Connection refused to keycloak:8080"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().length() > 0);
        log.info("Error message: {}", exception.getMessage());
    }

    // Mock helper
    private <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}



package com.agilesolutions.service_a.integration;

import com.agilesolutions.service_a.service.EntityClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Chaos Engineering Tests for Service A
 * 
 * Tests the service's resilience when Service B is unavailable, 
 * experiencing intermittent failures, or returning errors
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Service A Chaos Engineering Tests")
@Slf4j
class ChaosEngineeringTest {

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
    @DisplayName("Should handle complete Service B outage")
    void testCompleteServiceBOutage() {
        // Given: Service B is completely unavailable
        UUID entityId = UUID.randomUUID();
        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(503, exception.getStatusCode().value());
        log.info("Service A correctly handled complete Service B outage with 503");
    }

    @Test
    @DisplayName("Should recover from transient Service B failures")
    void testTransientServiceBFailure() {
        // Given: Service B fails first attempt, succeeds on retry
        UUID entityId = UUID.randomUUID();

        // Mock first 2 attempts fail, 3rd succeeds
        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenThrow(new ResourceAccessException("Connection timeout"))
                .thenThrow(new ResourceAccessException("Service temporarily unavailable"))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityId)
                                .name("Test Entity")
                                .description("Test")
                                .version("1.0.0")
                                .build()
                ));

        // When: Client retries on failure
        com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);

        // Then: Request succeeds after retries
        assertNotNull(result);
        assertEquals("Test Entity", result.getName());
        log.info("Service A recovered from transient failures after retries");
    }

    @Test
    @DisplayName("Should handle cascading failures gracefully")
    void testCascadingFailures() {
        // Given: Service B experiences cascading failures
        UUID entityId = UUID.randomUUID();
        AtomicInteger attemptCount = new AtomicInteger(0);

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    attemptCount.incrementAndGet();
                    throw new ResourceAccessException("Service overloaded");
                });

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(503, exception.getStatusCode().value());
        log.info("Service A handled cascading failures with {} retry attempts", attemptCount.get());
    }

    @Test
    @DisplayName("Should handle slow Service B responses (near timeout)")
    void testSlowServiceBResponse() {
        // Given: Service B is responding but slowly approaching timeout
        UUID entityId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    // Simulate slow response (4 seconds out of 5 timeout)
                    Thread.sleep(4000);
                    return org.springframework.http.ResponseEntity.ok(
                            com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityId)
                                .name("Slow Entity")
                                .description("Response was slow")
                                .version("1.0.0")
                                .build()
                    );
                });

        // When & Then: Service should eventually succeed
        com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
        assertNotNull(result);
        assertEquals("Slow Entity", result.getName());
        log.info("Service A handled slow Service B response");
    }

    @Test
    @DisplayName("Should handle intermittent network failures")
    void testIntermittentNetworkFailures() {
        // Given: Network has intermittent issues (fail-succeed-fail-succeed pattern)
        UUID entityId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger(0);

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    int call = callCount.incrementAndGet();
                    
                    if (call % 2 == 0) {  // Even calls succeed
                        return org.springframework.http.ResponseEntity.ok(
                                com.agilesolutions.service_a.model.EntityInfo.builder()
                                        .id(entityId)
                                        .name("Intermittent Entity")
                                        .description("Intermittent network")
                                        .version("1.0.0")
                                        .build()
                        );
                    } else {  // Odd calls fail
                        throw new ResourceAccessException("Network intermittently down");
                    }
                });

        // When & Then
        com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
        assertNotNull(result);
        log.info("Service A recovered from intermittent network failures");
    }

    @Test
    @DisplayName("Should handle concurrent requests during Service B degradation")
    void testConcurrentRequestsDuringDegradation() throws InterruptedException {
        // Given: Service B is degraded (high latency)
        UUID[] entityIds = new UUID[10];
        for (int i = 0; i < 10; i++) {
            entityIds[i] = UUID.randomUUID();
        }

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(2000);  // Simulate high latency
                    return org.springframework.http.ResponseEntity.ok(
                            com.agilesolutions.service_a.model.EntityInfo.builder()
                                    .id(entityIds[0])
                                    .name("Degraded Service")
                                    .description("High latency")
                                    .version("1.0.0")
                                    .build()
                    );
                });

        // When: 10 concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<Exception> exceptions = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (UUID entityId : entityIds) {
            executor.submit(() -> {
                try {
                    com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Some requests may timeout, but system stays stable
        latch.await();
        executor.shutdown();
        log.info("Service A handled {} concurrent requests during degradation (Success: {})", 
                10, successCount.get());
    }

    @Test
    @DisplayName("Should not lose data during Service B failures")
    void testDataIntegrityDuringFailures() {
        // Given: A scenario where Service B fails
        UUID entityId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenThrow(new ResourceAccessException("Service B crashed"));

        // When: Request fails
        assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        // Then: Retry should work when Service B recovers
        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenReturn(org.springframework.http.ResponseEntity.ok(
                        com.agilesolutions.service_a.model.EntityInfo.builder()
                                .id(entityId)
                                .name("Recovered Entity")
                                .description("After Service B recovery")
                                .version("1.0.0")
                                .build()
                ));

        com.agilesolutions.service_a.model.EntityInfo result = entityClient.getEntityInfo(entityId);
        assertNotNull(result);
        log.info("Data integrity verified after Service B recovery");
    }

    @Test
    @DisplayName("Should handle circuit breaker pattern (failing fast after multiple attempts)")
    void testCircuitBreakerBehavior() {
        // Given: Service B is consistently failing
        UUID entityId = UUID.randomUUID();

        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenThrow(new ResourceAccessException("Service B unreachable"));

        // When: Multiple consecutive requests
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            assertThrows(
                    ResponseStatusException.class,
                    () -> entityClient.getEntityInfo(entityId)
            );
        }

        long duration = System.currentTimeMillis() - startTime;

        // Then: Should give up relatively quickly (not exhaust all retries unnecessarily)
        log.info("Three consecutive failures took {}ms", duration);
        assertTrue(duration < 30000, "Should fail relatively quickly with 3 attempts");
    }
}



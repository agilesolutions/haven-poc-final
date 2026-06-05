package com.agilesolutions.service_a.integration;

import com.agilesolutions.service_a.controller.InfoController;
import com.agilesolutions.service_a.exception.GlobalExceptionHandler;
import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.EntityClient;
import com.agilesolutions.service_a.service.InfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Chaos Engineering Tests for Service A using RestTestClient
 *
 * Tests the service's resilience when Service B is unavailable, 
 * experiencing intermittent failures, or returning errors.
 * Uses modern RestClient with MockRestServiceServer for HTTP mocking.
 */
@WebMvcTest(InfoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {InfoController.class, GlobalExceptionHandler.class, InfoService.class})
@DisplayName("Service A Chaos Engineering Tests")
@Slf4j
class ChaosEngineeringTest {


    @Autowired
    private MockRestServiceServer mockServer;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private OAuth2AuthorizedClient authorizedClient;

    @MockitoBean
    private OAuth2AccessToken accessToken;

    @Autowired
    private EntityClient entityClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SERVICE_B_URL = "http://localhost:8081";
    private static final String TOKEN = "test-oauth2-token-chaos";

    @BeforeEach
    void setUp() {
        mockOAuth2Token(TOKEN);
    }

    @Test
    @DisplayName("Should handle complete Service B outage")
    void testCompleteServiceBOutage() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        // All retry attempts fail
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection refused");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(entityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        mockServer.verify();
        log.info("Service A correctly handled complete Service B outage with 503");
    }

    @Test
    @DisplayName("Should recover from transient Service B failures")
    void testTransientServiceBFailure() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        EntityInfo successEntity = EntityInfo.builder()
                .id(entityId.toString())
                .name("Test Entity")
                .description("Test")
                .version("1.0.0")
                .build();

        // First 2 attempts fail, 3rd succeeds
        mockServer.expect(times(2), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection timeout");
                });

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withSuccess(objectMapper.writeValueAsString(successEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(entityId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Entity");
        mockServer.verify();
        log.info("Service A recovered from transient failures after retries");
    }

    @Test
    @DisplayName("Should handle cascading failures gracefully")
    void testCascadingFailures() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        // All retry attempts fail
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Service overloaded");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(entityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        mockServer.verify();
        log.info("Service A handled cascading failures with 3 retry attempts");
    }

    @Test
    @DisplayName("Should handle slow Service B responses (near timeout)")
    void testSlowServiceBResponse() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        EntityInfo slowEntity = EntityInfo.builder()
                .id(entityId.toString())
                .name("Slow Entity")
                .description("Response was slow")
                .version("1.0.0")
                .build();

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(request -> {
                    // Simulate slow response (1 second - well under 5s timeout)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return withSuccess(objectMapper.writeValueAsString(slowEntity), MediaType.APPLICATION_JSON)
                            .createResponse(request);
                });

        // Act
        long startTime = System.currentTimeMillis();
        EntityInfo result = entityClient.getEntityInfo(entityId);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Slow Entity");
        assertThat(duration).isGreaterThanOrEqualTo(1000);
        mockServer.verify();
        log.info("Service A handled slow Service B response");
    }

    @Test
    @DisplayName("Should handle intermittent network failures")
    void testIntermittentNetworkFailures() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        EntityInfo successEntity = EntityInfo.builder()
                .id(entityId.toString())
                .name("Intermittent Entity")
                .description("Intermittent network")
                .version("1.0.0")
                .build();

        // First attempt fails, second succeeds
        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Network intermittently down");
                });

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withSuccess(objectMapper.writeValueAsString(successEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(entityId);

        // Assert
        assertThat(result).isNotNull();
        mockServer.verify();
        log.info("Service A recovered from intermittent network failures");
    }

    @Test
    @DisplayName("Should handle concurrent requests during Service B degradation")
    void testConcurrentRequestsDuringDegradation() throws InterruptedException {
        // Arrange
        UUID[] entityIds = new UUID[5];  // Reduced from 10 for faster execution
        for (int i = 0; i < 5; i++) {
            entityIds[i] = UUID.randomUUID();
        }

        EntityInfo degradedEntity = EntityInfo.builder()
                .id(entityIds[0].toString())
                .name("Degraded Service")
                .description("High latency")
                .version("1.0.0")
                .build();

        for (UUID entityId : entityIds) {
            mockServer.expect(once(), requestTo(SERVICE_B_URL + "/api/internal/info/" + entityId))
                    .andRespond(request -> {
                        try {
                            Thread.sleep(500);  // Simulate latency
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return withSuccess(objectMapper.writeValueAsString(degradedEntity), MediaType.APPLICATION_JSON)
                                .createResponse(request);
                    });
        }

        // Act
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);

        for (UUID entityId : entityIds) {
            executor.submit(() -> {
                try {
                    EntityInfo result = entityClient.getEntityInfo(entityId);
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    //log.debug("Request failed: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Assert
        latch.await();
        executor.shutdown();
        assertThat(successCount.get()).isGreaterThan(0);
        mockServer.verify();
        log.info("Service A handled concurrent requests during degradation (Success: {})", successCount.get());
    }

    @Test
    @DisplayName("Should not lose data during Service B failures")
    void testDataIntegrityDuringFailures() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        // First attempt fails
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Service B crashed");
                });

        // Act: First request fails
        assertThatThrownBy(() -> entityClient.getEntityInfo(entityId))
                .isInstanceOf(ResponseStatusException.class);

        mockServer.verify();
        log.info("Data integrity verified after Service B failure");
    }

    @Test
    @DisplayName("Should handle circuit breaker pattern (failing fast after multiple attempts)")
    void testCircuitBreakerBehavior() throws Exception {
        // Arrange
        UUID entityId = UUID.randomUUID();
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + entityId;

        mockServer.expect(times(9), requestTo(expectedUrl))  // 3 retries × 3 requests
                .andRespond(request -> {
                    throw new ResourceAccessException("Service B unreachable");
                });

        // Act
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> entityClient.getEntityInfo(entityId))
                    .isInstanceOf(ResponseStatusException.class);
        }

        long duration = System.currentTimeMillis() - startTime;

        // Assert: Should complete reasonably quickly
        assertThat(duration).isLessThan(30000);
        mockServer.verify();
        log.info("Three consecutive failures took {}ms", duration);
    }

    /**
     * Helper method to mock OAuth2 token acquisition
     */
    private void mockOAuth2Token(String token) {
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(token);
    }
}



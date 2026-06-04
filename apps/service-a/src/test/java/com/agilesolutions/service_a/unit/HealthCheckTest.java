package com.agilesolutions.service_a.unit;

import com.agilesolutions.service_a.controller.InfoController;
import com.agilesolutions.service_a.exception.GlobalExceptionHandler;
import com.agilesolutions.service_a.service.InfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Health Check Endpoint Tests for Service A
 * 
 * Tests endpoints that verify service health status including
 * application health, database connectivity, and dependency status
 */
@WebMvcTest(excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
//@AutoConfigureMockMvc(addFilters = false) // Disable security filters to reach the endpoint
@SpringJUnitConfig(classes = {GlobalExceptionHandler.class})
@DisplayName("Service A Health Check Tests")
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=health,info,metrics", // Expose for test
        "management.endpoint.health.show-details=always"
})
@Slf4j
class HealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InfoService infoService;

    @Test
    @DisplayName("Should respond to health check with 200 OK")
    void testHealthCheckEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        log.info("Health check endpoint responded with UP");
    }

    @Test
    @DisplayName("Should expose health details endpoint")
    void testHealthDetailsEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").isMap());

        log.info("Health details endpoint accessible");
    }

    @Test
    @DisplayName("Should include liveness probe")
    void testLivenessProbe() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health/liveness"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        log.info("Liveness probe healthy");
    }

    @Test
    @DisplayName("Should include readiness probe")
    void testReadinessProbe() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health/readiness"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        log.info("Readiness probe healthy");
    }

    @Test
    @DisplayName("Should expose application info endpoint")
    void testInfoEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("service-a"));

        log.info("Application info exposed");
    }

    @Test
    @DisplayName("Should expose metrics endpoint")
    void testMetricsEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/metrics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItems(
                        "jvm.memory.used",
                        "process.cpu.usage",
                        "http.server.requests"
                )));

        log.info("Metrics endpoint accessible");
    }

    @Test
    @DisplayName("Should expose HTTP request metrics")
    void testHttpMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/metrics/http.server.requests"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("http.server.requests"))
                .andExpect(jsonPath("$.measurements", hasSize(greaterThan(0))));

        log.info("HTTP metrics available");
    }

    @Test
    @DisplayName("Should expose traces endpoint")
    void testTracesEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/traces"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("Traces endpoint available");
    }

    @Test
    @DisplayName("Should return health status based on application startup")
    void testHealthStatusAfterStartup() throws Exception {
        // When: Application is started
        // Then: Health should immediately be UP
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        log.info("Service A is healthy and ready");
    }

    @Test
    @DisplayName("Should include component health status")
    void testComponentHealthStatus() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").exists())
                .andReturn();

        log.info("Component health status available");
    }

    @Test
    @DisplayName("Should verify health endpoint is not authenticated")
    void testHealthEndpointPublicAccess() throws Exception {
        // When: Accessing health without authentication
        // Then: Should still succeed (health endpoints are typically public)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        log.info("Health endpoint is publicly accessible");
    }

    @Test
    @DisplayName("Should monitor application startup status")
    void testApplicationStartupMonitoring() throws Exception {
        // Given: Application has started
        // When: Check application startup metrics
        // Then: Should indicate successful startup
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        log.info("Application startup monitoring OK");
    }

    @Test
    @DisplayName("Should provide detailed health information")
    void testDetailedHealthInformation() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        log.info("Detailed health information available");
    }

    @Test
    @DisplayName("Should expose JVM metrics")
    void testJvmMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.baseUnit").value("bytes"));

        log.info("JVM metrics available");
    }

    @Test
    @DisplayName("Should expose process metrics")
    void testProcessMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/metrics/process.cpu.usage"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("process.cpu.usage"));

        log.info("Process metrics available");
    }

    @Test
    @DisplayName("Should respond quickly to health checks")
    void testHealthCheckPerformance() throws Exception {
        // When: Measure health check response time
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Then: Should respond within reasonable time
        assertTrue(duration < 500, "Health check should respond within 500ms, took: " + duration + "ms");
        log.info("Health check responded in {}ms", duration);
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}



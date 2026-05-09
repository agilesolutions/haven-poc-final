package com.agilesolutions.service_a.unit;

import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.EntityClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Service A error handling
 * 
 * Tests error handling for service unavailability, timeouts, and auth failures
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Service A Error Handling Tests")
@Slf4j
class ErrorHandlingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @InjectMocks
    private EntityClient entityClient;

    @Test
    @DisplayName("Should return 503 Service Unavailable when Service B is unreachable")
    void testServiceBUnavailable() {
        // Given
        UUID entityId = UUID.randomUUID();
        String url = "http://localhost:8081/api/internal/info/" + entityId;
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        log.info("Service A correctly returned 503 for unavailable Service B");
    }

    @Test
    @DisplayName("Should return 504 Gateway Timeout when Service B request times out")
    void testServiceBTimeout() {
        // Given
        UUID entityId = UUID.randomUUID();
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new ResourceAccessException("Connection timeout", new SocketTimeoutException("Read timeout")));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatusCode());
        log.info("Service A correctly returned 504 for Service B timeout");
    }

    @Test
    @DisplayName("Should return 404 Not Found when entity doesn't exist in Service B")
    void testEntityNotFound() {
        // Given
        UUID entityId = UUID.randomUUID();
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Entity not found"));

        // When & Then
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        log.info("Service A correctly returned 404 for non-existent entity");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when OAuth2 token is invalid")
    void testUnauthorizedToken() {
        // Given
        UUID entityId = UUID.randomUUID();
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // When & Then
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        log.info("Service A correctly returned 401 for invalid token");
    }

    @Test
    @DisplayName("Should handle network socket errors gracefully")
    void testNetworkSocketError() {
        // Given
        UUID entityId = UUID.randomUUID();
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new ResourceAccessException("Connection reset by peer"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        log.info("Service A correctly handled network socket error");
    }

    @Test
    @DisplayName("Should provide meaningful error messages")
    void testErrorMessageQuality() {
        // Given
        UUID entityId = UUID.randomUUID();
        
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new ResourceAccessException("Unable to connect to host"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().length() > 0);
        log.info("Error message: {}", exception.getReason());
    }

    @Test
    @DisplayName("Should distinguish between different HTTP error codes")
    void testDistinctHttpErrors() {
        // Test 500 error
        when(restTemplate.exchange(
                contains("api/internal/info"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(EntityInfo.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        UUID entityId = UUID.randomUUID();
        
        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> entityClient.getEntityInfo(entityId)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        log.info("Service A correctly identified 500 error");
    }
}



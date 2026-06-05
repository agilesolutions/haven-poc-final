package com.agilesolutions.service_a.unit;

import com.agilesolutions.service_a.config.OAuth2ClientConfig;
import com.agilesolutions.service_a.config.RestClientConfig;
import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.EntityClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.MockRestServiceServer.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for Service A error handling using RestTestClient
 *
 * Tests error handling for service unavailability, timeouts, and auth failures
 * using modern RestClient with MockRestServiceServer for HTTP mocking.
 */
@RestClientTest(EntityClient.class)
@ContextConfiguration(classes = {EntityClient.class, RestClientConfig.class, OAuth2ClientConfig.class, ObjectMapper.class})
@DisplayName("Service A Error Handling Tests")
@Slf4j
class ErrorHandlingTest {

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

    private UUID testEntityId;
    private static final String SERVICE_B_URL = "http://localhost:8081";
    private static final String TOKEN = "test-oauth2-token-error";

    @BeforeEach
    void setUp() {
        testEntityId = UUID.randomUUID();
        // Mock OAuth2 token acquisition
        mockOAuth2Token(TOKEN);
    }

    @Test
    @DisplayName("Should return 503 Service Unavailable when Service B is unreachable")
    void testServiceBUnavailable() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        // All retry attempts fail with connection error
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection refused");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });

        mockServer.verify();
        log.info("Service A correctly returned 503 for unavailable Service B");
    }

    @Test
    @DisplayName("Should return 504 Gateway Timeout when Service B request times out")
    void testServiceBTimeout() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection timeout");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                });

        mockServer.verify();
        log.info("Service A correctly returned 504 for Service B timeout");
    }

    @Test
    @DisplayName("Should return 404 Not Found when entity doesn't exist in Service B")
    void testEntityNotFound() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"Entity not found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        mockServer.verify();
        log.info("Service A correctly returned 404 for non-existent entity");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when OAuth2 token is invalid")
    void testUnauthorizedToken() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"message\":\"Invalid token\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);

        mockServer.verify();
        log.info("Service A correctly returned 401 for invalid token");
    }

    @Test
    @DisplayName("Should handle network socket errors gracefully")
    void testNetworkSocketError() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection reset by peer");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });

        mockServer.verify();
        log.info("Service A correctly handled network socket error");
    }

    @Test
    @DisplayName("Should provide meaningful error messages")
    void testErrorMessageQuality() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Unable to connect to host");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getReason()).isNotNull().isNotEmpty();
                });

        mockServer.verify();
        log.info("Error message quality verified");
    }

    @Test
    @DisplayName("Should distinguish between different HTTP error codes")
    void testDistinctHttpErrors() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"message\":\"Database error\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class);

        mockServer.verify();
        log.info("Service A correctly identified 500 error");
    }

    /**
     * Helper method to mock OAuth2 token acquisition
     */
    private void mockOAuth2Token(String token) {
        // Token is mocked via OAuth2AuthorizedClientManager bean
        // Actual token injection happens in RestClient fluent API
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(token);
    }
}



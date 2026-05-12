package com.agilesolutions.service_a.service;

import com.agilesolutions.service_a.model.EntityInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.MockRestServiceServer.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for EntityClient using RestTestClient (Spring Boot 4.x)
 *
 * Tests the HTTP communication layer between Service A and Service B,
 * including OAuth2 token handling, error scenarios, and retry logic.
 *
 * RestTestClient provides a modern, composable way to test HTTP clients
 * without needing actual HTTP servers.
 */
@RestClientTest(EntityClient.class)
@Slf4j
@DisplayName("EntityClient Unit Tests with RestTestClient")
class EntityClientTest {

    @Autowired
    private RestClient restClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @MockBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    private EntityClient entityClient;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testEntityId;
    private EntityInfo testEntity;
    private static final String SERVICE_B_URL = "http://localhost:8081";
    private static final String TOKEN = "test-oauth2-token-123";

    @BeforeEach
    void setUp() {
        testEntityId = UUID.randomUUID();
        testEntity = EntityInfo.builder()
                .id(testEntityId.toString())
                .name("Test Entity")
                .description("Entity for testing")
                .version("1.0.0")
                .build();

        // Mock OAuth2 token acquisition
        mockOAuth2Token(TOKEN);
    }

    @Test
    @DisplayName("Should successfully fetch entity from Service B with OAuth2 token")
    void testFetchEntitySuccess() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(testEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(testEntityId);

        // Assert
        assertThat(result)
                .isNotNull()
                .extracting("id", "name", "version")
                .containsExactly(testEntityId.toString(), "Test Entity", "1.0.0");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle entity not found (404) from Service B")
    void testFetchEntityNotFound() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"Entity not found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("404");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle Service B unavailable (503)")
    void testFetchEntityServiceUnavailable() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        // Mock all 3 retry attempts returning 503
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("{\"message\":\"Service unavailable\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Service B is unavailable");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should retry on connection timeout with exponential backoff")
    void testFetchEntityConnectionTimeout() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        // First 2 attempts timeout, 3rd succeeds
        mockServer.expect(times(2), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection timeout");
                });

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withSuccess(objectMapper.writeValueAsString(testEntity), MediaType.APPLICATION_JSON));

        // Act - Should eventually succeed after retries
        long startTime = System.currentTimeMillis();
        EntityInfo result = entityClient.getEntityInfo(testEntityId);
        long duration = System.currentTimeMillis() - startTime;

        // Assert - Verify backoff was applied (at least 300ms for 2 retries: 100ms + 200ms)
        assertThat(result).isNotNull();
        assertThat(duration).isGreaterThanOrEqualTo(300);

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle 500 Internal Server Error from Service B")
    void testFetchEntityInternalServerError() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"message\":\"Database error\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Service B is unavailable");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should include correct OAuth2 Bearer token in request header")
    void testOAuth2TokenInHeader() throws Exception {
        // Arrange
        String customToken = "custom-bearer-token-xyz";
        mockOAuth2Token(customToken);

        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andExpect(header("Authorization", "Bearer " + customToken))
                .andRespond(withSuccess(objectMapper.writeValueAsString(testEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(testEntityId);

        // Assert
        assertThat(result).isNotNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should set correct content type and accept headers")
    void testRequestHeaders() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("User-Agent", "Service-A/1.0.0"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(testEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(testEntityId);

        // Assert
        assertThat(result).isNotNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should fetch entity by name")
    void testFetchEntityByName() throws Exception {
        // Arrange
        String entityName = "Test Entity";
        String expectedUrl = SERVICE_B_URL + "/api/internal/info?name=" + entityName;

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withSuccess(objectMapper.writeValueAsString(testEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfoByName(entityName);

        // Assert
        assertThat(result)
                .isNotNull()
                .extracting("name")
                .isEqualTo("Test Entity");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should fail after max retries exceeded")
    void testMaxRetriesExceeded() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        // All 3 retry attempts fail with 503
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle malformed JSON response")
    void testMalformedJsonResponse() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(withSuccess("{invalid json", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(Exception.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle Gateway Timeout on repeated timeouts")
    void testGatewayTimeoutAfterRetries() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        // All attempts timeout
        mockServer.expect(times(3), requestTo(expectedUrl))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection timeout");
                });

        // Act & Assert
        assertThatThrownBy(() -> entityClient.getEntityInfo(testEntityId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GATEWAY_TIMEOUT");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should parse entity response correctly")
    void testEntityResponseParsing() throws Exception {
        // Arrange
        String expectedUrl = SERVICE_B_URL + "/api/internal/info/" + testEntityId;

        EntityInfo detailedEntity = EntityInfo.builder()
                .id(testEntityId.toString())
                .name("Detailed Entity")
                .description("A comprehensive entity description with special characters: áéíóú")
                .version("2.3.1")
                .build();

        mockServer.expect(once(), requestTo(expectedUrl))
                .andRespond(withSuccess(objectMapper.writeValueAsString(detailedEntity), MediaType.APPLICATION_JSON));

        // Act
        EntityInfo result = entityClient.getEntityInfo(testEntityId);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(entity -> {
                    assertThat(entity.getId()).isEqualTo(testEntityId.toString());
                    assertThat(entity.getName()).isEqualTo("Detailed Entity");
                    assertThat(entity.getDescription()).contains("special characters");
                    assertThat(entity.getVersion()).isEqualTo("2.3.1");
                });

        mockServer.verify();
    }

    /**
     * Helper method to mock OAuth2 token acquisition
     *
     * @param token the token to return when acquiring OAuth2 credentials
     */
    private void mockOAuth2Token(String token) {
        // In a real test, you would mock the OAuth2AuthorizedClientManager
        // For RestTestClient, the token is typically included in the request headers
        // This is a placeholder for the OAuth2 setup
    }
}


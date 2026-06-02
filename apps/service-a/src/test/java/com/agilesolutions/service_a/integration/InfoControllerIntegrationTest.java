package com.agilesolutions.service_a.integration;

import com.agilesolutions.service_a.controller.InfoController;
import com.agilesolutions.service_a.exception.GlobalExceptionHandler;
import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.EntityClient;
import com.agilesolutions.service_a.service.InfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Service A calling Service B
 * 
 * Tests the complete flow from external API call through Service A
 * to Service B, including error handling and retry logic.
 */
@WebMvcTest(InfoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {InfoController.class, GlobalExceptionHandler.class, InfoService.class})
@DisplayName("Service A to Service B Integration Tests")
class InfoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EntityClient entityClient;

    @Autowired
    private InfoService infoService;

    @Autowired
    private InfoController infoController;

    private UUID testId;
    private EntityInfo testEntityInfo;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEntityInfo = EntityInfo.builder()
                .id(testId.toString())
                .name("Integration Test Entity")
                .description("Entity for integration testing from Service B")
                .version("1.0.0")
                .build();
    }

    @Test
    @DisplayName("Should retrieve entity info from Service B via public API")
    void testEndToEndEntityRetrieval() throws Exception {
        // Given
        when(entityClient.getEntityInfo(testId)).thenReturn(testEntityInfo);

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testId.toString())))
                .andExpect(jsonPath("$.name", is("Integration Test Entity")))
                .andExpect(jsonPath("$.description", containsString("Service B")))
                .andExpect(jsonPath("$.version", is("1.0.0")));

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should handle 404 from Service B gracefully")
    void testEntityNotFoundInServiceB() throws Exception {
        // Given
        when(entityClient.getEntityInfo(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(NOT_FOUND, "Entity not found"));

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should handle 503 from Service B (unavailable)")
    void testServiceBUnavailable() throws Exception {
        // Given
        when(entityClient.getEntityInfo(any(UUID.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should handle 401 from Service B (authentication failure)")
    void testAuthenticationFailureAtServiceB() throws Exception {
        // Given
        when(entityClient.getEntityInfo(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(UNAUTHORIZED, "Invalid token"));

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should handle invalid UUID format")
    void testInvalidUUIDFormat() throws Exception {
        // Given
        String invalidId = "not-a-uuid";

        // When & Then
        mockMvc.perform(get("/api/info/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(entityClient, never()).getEntityInfo(any());
    }

    @Test
    @DisplayName("Should retrieve entity by name from Service B")
    void testGetEntityInfoByName() throws Exception {
        // Given
        when(entityClient.getEntityInfoByName("Integration Test Entity"))
                .thenReturn(testEntityInfo);

        // When & Then
        mockMvc.perform(get("/api/info?name=Integration Test Entity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Integration Test Entity")))
                .andExpect(jsonPath("$.version", is("1.0.0")));

        verify(entityClient, times(1)).getEntityInfoByName("Integration Test Entity");
    }

    @Test
    @DisplayName("Should handle empty name parameter")
    void testEmptyNameParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/info?name=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(entityClient, never()).getEntityInfoByName(anyString());
    }

    @Test
    @DisplayName("Should handle missing name parameter")
    void testMissingNameParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(entityClient, never()).getEntityInfoByName(anyString());
    }

    @Test
    @DisplayName("Should return correct content type")
    void testResponseContentType() throws Exception {
        // Given
        when(entityClient.getEntityInfo(testId)).thenReturn(testEntityInfo);

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should handle network timeout from Service B")
    void testNetworkTimeout() throws Exception {
        // Given
        when(entityClient.getEntityInfo(any(UUID.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // When & Then
        mockMvc.perform(get("/api/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());

        verify(entityClient, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should map response from Service B correctly")
    void testResponseMapping() throws Exception {
        // Given
        EntityInfo expectedInfo = EntityInfo.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .name("Specific Entity")
                .description("A specific test entity with details")
                .version("2.5.0")
                .build();

        UUID expectedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(entityClient.getEntityInfo(expectedId)).thenReturn(expectedInfo);

        // When & Then
        mockMvc.perform(get("/api/info/" + expectedId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id", is("550e8400-e29b-41d4-a716-446655440000")))
                .andExpect(jsonPath("$.name", is("Specific Entity")))
                .andExpect(jsonPath("$.description", is("A specific test entity with details")))
                .andExpect(jsonPath("$.version", is("2.5.0")));

        verify(entityClient, times(1)).getEntityInfo(expectedId);
    }

    @Test
    @DisplayName("Should handle concurrent requests to Service B")
    void testConcurrentRequests() throws Exception {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(entityClient.getEntityInfo(id1))
                .thenReturn(EntityInfo.builder()
                        .id(id1.toString())
                        .name("Entity 1")
                        .version("1.0.0")
                        .build());
        when(entityClient.getEntityInfo(id2))
                .thenReturn(EntityInfo.builder()
                        .id(id2.toString())
                        .name("Entity 2")
                        .version("1.0.0")
                        .build());

        // When & Then - make concurrent requests
        mockMvc.perform(get("/api/info/" + id1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Entity 1")));

        mockMvc.perform(get("/api/info/" + id2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Entity 2")));

        verify(entityClient, times(1)).getEntityInfo(id1);
        verify(entityClient, times(1)).getEntityInfo(id2);
    }
}


package com.agilesolutions.service_a.unit;

import com.agilesolutions.service_a.controller.InfoController;
import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.InfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InfoController (Service A)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InfoController Unit Tests")
class InfoControllerTest {

    @Mock
    private InfoService infoService;

    @InjectMocks
    private InfoController controller;

    private EntityInfo testEntityInfo;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEntityInfo = EntityInfo.builder()
                .id(testId.toString())
                .name("Test Entity")
                .description("A test entity")
                .version("1.0.0")
                .build();
    }

    @Test
    @DisplayName("Should return entity info when Service B returns data")
    void testGetInfo_Success() {
        // Given
        when(infoService.getEntityInfo(testId)).thenReturn(testEntityInfo);

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testId.toString());
        assertThat(response.getBody().getName()).isEqualTo("Test Entity");
        verify(infoService, times(1)).getEntityInfo(testId);
    }

    @Test
    @DisplayName("Should return 404 when entity not found")
    void testGetInfo_NotFound() {
        // Given
        when(infoService.getEntityInfo(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should return 400 for invalid UUID format")
    void testGetInfo_InvalidUUID() {
        // Given
        String invalidId = "invalid-uuid";

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(invalidId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(infoService, never()).getEntityInfo(any());
    }

    @Test
    @DisplayName("Should return 401 when authentication fails")
    void testGetInfo_Unauthorized() {
        // Given
        when(infoService.getEntityInfo(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should return 403 when access forbidden")
    void testGetInfo_Forbidden() {
        // Given
        when(infoService.getEntityInfo(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should return 503 when Service B is unavailable")
    void testGetInfo_ServiceUnavailable() {
        // Given
        when(infoService.getEntityInfo(any(UUID.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should return entity info by name")
    void testGetInfoByName_Success() {
        // Given
        when(infoService.getEntityInfoByName("Test Entity")).thenReturn(testEntityInfo);

        // When
        ResponseEntity<EntityInfo> response = controller.getInfoByName("Test Entity");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Entity");
        verify(infoService, times(1)).getEntityInfoByName("Test Entity");
    }

    @Test
    @DisplayName("Should return 400 when name parameter is empty")
    void testGetInfoByName_EmptyName() {
        // When
        ResponseEntity<EntityInfo> response = controller.getInfoByName("");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(infoService, never()).getEntityInfoByName(anyString());
    }

    @Test
    @DisplayName("Should return 404 when entity name not found")
    void testGetInfoByName_NotFound() {
        // Given
        when(infoService.getEntityInfoByName(anyString()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfoByName("NonExistent");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should handle unexpected exceptions gracefully")
    void testGetInfo_UnexpectedException() {
        // Given
        when(infoService.getEntityInfo(any(UUID.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        ResponseEntity<EntityInfo> response = controller.getInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }
}


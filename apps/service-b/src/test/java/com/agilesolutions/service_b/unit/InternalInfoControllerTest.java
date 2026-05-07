package com.agilesolutions.service_b.unit;

import com.agilesolutions.service_b.controller.InternalInfoController;
import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.model.EntityInfo;
import com.agilesolutions.service_b.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InternalInfoController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InternalInfoController Unit Tests")
class InternalInfoControllerTest {

    @Mock
    private EntityService entityService;

    @InjectMocks
    private InternalInfoController controller;

    private Entity testEntity;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEntity = Entity.builder()
                .id(testId)
                .name("Test Entity")
                .description("A test entity")
                .version("1.0.0")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return EntityInfo when entity is found by ID")
    void testGetInternalInfo_Success() {
        // Given
        when(entityService.findActiveById(testId)).thenReturn(testEntity);

        // When
        ResponseEntity<EntityInfo> response = controller.getInternalInfo(testId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testId.toString());
        assertThat(response.getBody().getName()).isEqualTo("Test Entity");
        assertThat(response.getBody().getDescription()).isEqualTo("A test entity");
        assertThat(response.getBody().getVersion()).isEqualTo("1.0.0");
        verify(entityService, times(1)).findActiveById(testId);
    }

    @Test
    @DisplayName("Should return 404 when entity not found by ID")
    void testGetInternalInfo_NotFound() {
        // Given
        when(entityService.findActiveById(any(UUID.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Entity not found"));

        // When & Then
        assertThatThrownBy(() -> controller.getInternalInfo(testId.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Entity not found");
        verify(entityService, times(1)).findActiveById(testId);
    }

    @Test
    @DisplayName("Should return 400 when UUID format is invalid")
    void testGetInternalInfo_InvalidUUID() {
        // Given
        String invalidId = "invalid-uuid";

        // When
        ResponseEntity<EntityInfo> response = controller.getInternalInfo(invalidId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("Should return EntityInfo when entity is found by name")
    void testGetInternalInfoByName_Success() {
        // Given
        when(entityService.findByName("Test Entity")).thenReturn(testEntity);

        // When
        ResponseEntity<EntityInfo> response = controller.getInternalInfoByName("Test Entity");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Entity");
        verify(entityService, times(1)).findByName("Test Entity");
    }

    @Test
    @DisplayName("Should return 404 when entity not found by name")
    void testGetInternalInfoByName_NotFound() {
        // Given
        when(entityService.findByName(anyString()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Entity not found"));

        // When & Then
        assertThatThrownBy(() -> controller.getInternalInfoByName("NonExistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Entity not found");
    }

    @Test
    @DisplayName("Should map entity properties correctly to EntityInfo")
    void testEntityToEntityInfoMapping() {
        // Given
        when(entityService.findActiveById(testId)).thenReturn(testEntity);

        // When
        ResponseEntity<EntityInfo> response = controller.getInternalInfo(testId.toString());

        // Then
        assertThat(response.getBody())
                .isNotNull()
                .extracting("id", "name", "description", "version")
                .containsExactly(
                        testId.toString(),
                        "Test Entity",
                        "A test entity",
                        "1.0.0"
                );
    }
}


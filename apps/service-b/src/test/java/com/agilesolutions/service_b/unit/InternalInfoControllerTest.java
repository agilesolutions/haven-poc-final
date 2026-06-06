package com.agilesolutions.service_b.unit;

import com.agilesolutions.service_b.controller.InternalInfoController;
import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.model.EntityInfo;
import com.agilesolutions.service_b.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Unit tests for InternalInfoController
 */
@WebMvcTest(InternalInfoController.class)
@DisplayName("InternalInfoController Unit Tests")
class InternalInfoControllerTest {

    @MockitoBean
    private EntityService entityService;

    @Autowired
    private MockMvc mockMvc;

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
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should return EntityInfo when entity is found by ID")
    void testGetInternalInfo_Success() throws Exception {
        // Given
        when(entityService.findActiveById(testId)).thenReturn(testEntity);

        // When
        mockMvc.perform(get("/internal/info/{id}", testId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.name").value("Test Entity"))
                .andExpect(jsonPath("$.description").value("A test entity"))
                .andExpect(jsonPath("$.version").value("1.0.0"));


    }

    @Test
    @DisplayName("Should return 404 when entity not found by ID")
    void testGetInternalInfo_NotFound() throws Exception {
        // Given
        when(entityService.findActiveById(any(UUID.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Entity not found"));

        // When & Then
        mockMvc.perform(get("/internal/info/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Entity not found"));
    }

    @Test
    @DisplayName("Should return 400 when UUID format is invalid")
    void testGetInternalInfo_InvalidUUID() throws Exception {
        // Given
        String invalidId = "invalid-uuid";

        // When & Then
        mockMvc.perform(get("/internal/info/{id}", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format: " + invalidId));

    }

    @Test
    @DisplayName("Should return EntityInfo when entity is found by name")
    void testGetInternalInfoByName_Success() throws Exception {
        // Given
        when(entityService.findByName("Test Entity")).thenReturn(testEntity);

        // When & Then
        mockMvc.perform(get("/internal/info/name/{name}", "Test Entity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.name").value("Test Entity"))
                .andExpect(jsonPath("$.description").value("A test entity"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("Should return 404 when entity not found by name")
    void testGetInternalInfoByName_NotFound() throws Exception {
        // Given
        when(entityService.findByName(anyString()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Entity not found"));

        // When & Then
        mockMvc.perform(get("/internal/info/name/{name}", "Nonexistent Entity"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Entity not found"));
    }

    @Test
    @DisplayName("Should map entity properties correctly to EntityInfo")
    void testEntityToEntityInfoMapping() throws Exception {
        // Given
        when(entityService.findActiveById(testId)).thenReturn(testEntity);

        // When & Then
        mockMvc.perform(get("/internal/info/{id}", testId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.name").value("Test Entity"))
                .andExpect(jsonPath("$.description").value("A test entity"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
}


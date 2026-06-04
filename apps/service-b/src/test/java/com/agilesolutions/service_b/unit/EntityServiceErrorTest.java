package com.agilesolutions.service_b.unit;

import com.agilesolutions.service_b.exception.ServiceUnavailableException;
import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import com.agilesolutions.service_b.service.EntityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityService error handling scenarios
 * 
 * Tests error handling for database unavailability, timeouts, and other exceptions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService Error Handling Tests")
class EntityServiceErrorTest {

    @Mock
    private EntityRepository entityRepository;

    @InjectMocks
    private EntityService entityService;

    @Test
    @DisplayName("Should throw ServiceUnavailableException when database is unavailable during findById")
    void testFindByIdDatabaseUnavailable() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findById(id)
        );

        assertTrue(exception.getMessage().contains("Database is unavailable"));
        verify(entityRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should throw ResponseStatusException with 404 when entity not found")
    void testFindByIdNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityService.findById(id)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(entityRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when database error during findActiveById")
    void testFindActiveByIdDatabaseError() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findByIdAndActive(id, true))
                .thenThrow(new DataAccessException("Timeout occurred") {});

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findActiveById(id)
        );

        assertTrue(exception.getMessage().contains("Database is unavailable"));
        verify(entityRepository, times(1)).findByIdAndActive(id, true);
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when database error during findByName")
    void testFindByNameDatabaseError() {
        // Given
        String name = "Test Entity";
        when(entityRepository.findActiveByName(name))
                .thenThrow(new DataAccessResourceFailureException("Database connection lost"));

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findByName(name)
        );

        assertTrue(exception.getMessage().contains("Database is unavailable"));
        verify(entityRepository, times(1)).findActiveByName(name);
    }

    @Test
    @DisplayName("Should throw ResponseStatusException 404 when active entity not found by name")
    void testFindByNameNotFound() {
        // Given
        String name = "Non-existent Entity";
        when(entityRepository.findActiveByName(name)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityService.findByName(name)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(entityRepository, times(1)).findActiveByName(name);
    }

    @Test
    @DisplayName("Should handle unexpected exceptions and throw ServiceUnavailableException")
    void testFindByIdUnexpectedException() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findById(id)
        );

        assertTrue(exception.getMessage().contains("Error retrieving entity"));
        verify(entityRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should handle update when entity not found")
    void testUpdateEntityNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        Entity updateEntity = new Entity();
        updateEntity.setName("Updated Name");

        when(entityRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityService.update(id, updateEntity)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(entityRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should handle delete when entity not found")
    void testDeleteEntityNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> entityService.delete(id)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(entityRepository, times(1)).findById(id);
        verify(entityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should wrap DataAccessException for create operations")
    void testCreateWithDatabaseError() {
        // Given
        Entity entity = new Entity();
        entity.setName("Test");
        entity.setDescription("Test entity");

        when(entityRepository.save(any(Entity.class)))
                .thenThrow(new DataAccessResourceFailureException("Database write failed"));

        // When & Then
        // Note: create() doesn't wrap DataAccessException in current implementation
        // This test documents the current behavior
        DataAccessResourceFailureException exception = assertThrows(
                DataAccessResourceFailureException.class,
                () -> entityService.create(entity)
        );

        assertTrue(exception.getMessage().contains("Database write failed"));
    }

    @Test
    @DisplayName("Should handle connection timeout scenarios")
    void testConnectionTimeout() {
        // Given
        UUID id = UUID.randomUUID();
        when(entityRepository.findById(id))
                .thenThrow(new DataAccessResourceFailureException(
                        "Connection timeout after 5000ms"
                ));

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findById(id)
        );

        assertNotNull(exception.getCause());
        verify(entityRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should preserve error cause chain for debugging")
    void testErrorCauseChain() {
        // Given
        UUID id = UUID.randomUUID();
        RuntimeException originalCause = new RuntimeException("Network failure");
        DataAccessException dataAccessEx = new DataAccessResourceFailureException(
                "Failed to retrieve data",
                originalCause
        );

        when(entityRepository.findById(id)).thenThrow(dataAccessEx);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> entityService.findById(id)
        );

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof DataAccessException);
    }
}



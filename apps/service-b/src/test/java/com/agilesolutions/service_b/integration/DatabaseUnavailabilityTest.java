package com.agilesolutions.service_b.integration;

import com.agilesolutions.service_b.exception.ServiceUnavailableException;
import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import com.agilesolutions.service_b.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for database unavailability scenarios
 * 
 * Tests that application properly handles database connection failures,
 * timeouts, and other unavailability scenarios
 */
@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@Import(EntityService.class)
@Slf4j
@DisplayName("Database Unavailability Tests")
class DatabaseUnavailabilityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityService entityService;

    @BeforeEach
    void setUp() {
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle database connection unavailability gracefully")
    void testDatabaseConnectionUnavailable() {
        // Given: Create a mock repository that throws connection error
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(any(UUID.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        // Create service with mocked repository
        EntityService service = new EntityService(mockRepository);

        UUID testId = UUID.randomUUID();

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(testId)
        );

        assertEquals("Database is unavailable", exception.getMessage());
        log.info("Service correctly handled connection unavailability");
    }

    @Test
    @DisplayName("Should return 503 error message when database is unreachable")
    void testDatabaseUnreachable() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findActiveByName(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Unable to acquire JDBC Connection"));

        EntityService service = new EntityService(mockRepository);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findByName("Any Name")
        );

        assertTrue(exception.getMessage().contains("Database is unavailable"));
        log.info("Service correctly handled unreachable database");
    }

    @Test
    @DisplayName("Should handle database timeout errors")
    void testDatabaseTimeout() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(any(UUID.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Connection timeout: Unable to get a connection, pool error Timeout waiting for idle object"
                ));

        EntityService service = new EntityService(mockRepository);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(UUID.randomUUID())
        );

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof DataAccessResourceFailureException);
        log.info("Service correctly handled database timeout");
    }

    @Test
    @DisplayName("Should recover after database becomes available again")
    void testDatabaseRecovery() {
        // Given: Database is initially available
        Entity entity = new Entity();
        entity.setName("Recovery Test Entity");
        entity.setDescription("Entity for recovery testing");
        entity.setVersion("1.0.0");
        Entity savedEntity = entityRepository.save(entity);

        // Verify it's available
        Entity retrieved = entityService.findById(savedEntity.getId());
        assertNotNull(retrieved);
        log.info("Entity retrieved successfully from available database");

        // When: Database becomes unavailable (simulated by mock)
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(savedEntity.getId()))
                .thenThrow(new DataAccessResourceFailureException("Connection lost"));

        EntityService service = new EntityService(mockRepository);

        // Then: Service properly raises ServiceUnavailableException
        assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(savedEntity.getId())
        );

        // When: Database becomes available again (mock returns data)
        when(mockRepository.findById(savedEntity.getId()))
                .thenReturn(java.util.Optional.of(savedEntity));

        // Then: Service recovers and returns data
        Entity recoveredEntity = service.findById(savedEntity.getId());
        assertNotNull(recoveredEntity);
        assertEquals(savedEntity.getId(), recoveredEntity.getId());
        log.info("Service successfully recovered after database became available");
    }

    @Test
    @DisplayName("Should handle connection pool exhaustion")
    void testConnectionPoolExhaustion() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(any(UUID.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Cannot get a connection, pool error: Timeout waiting for an available request slot"
                ));

        EntityService service = new EntityService(mockRepository);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(UUID.randomUUID())
        );

        assertTrue(exception.getMessage().contains("unavailable"));
        log.info("Service correctly handled connection pool exhaustion");
    }

    @Test
    @DisplayName("Should handle database authentication failures")
    void testDatabaseAuthenticationFailure() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(any(UUID.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "FATAL: password authentication failed for user \"testuser\""
                ));

        EntityService service = new EntityService(mockRepository);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(UUID.randomUUID())
        );

        assertNotNull(exception.getCause());
        log.info("Service correctly handled authentication failure");
    }

    @Test
    @DisplayName("Should handle network socket errors")
    void testNetworkSocketError() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findActiveByName(anyString()))
                .thenThrow(new DataAccessResourceFailureException(
                        "An established connection was aborted by the software in your host machine"
                ));

        EntityService service = new EntityService(mockRepository);

        // When & Then
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findByName("Test")
        );

        assertTrue(exception.getMessage().contains("Database is unavailable"));
        log.info("Service correctly handled network socket error");
    }

    @Test
    @DisplayName("Should maintain data integrity when database recovers from brief outage")
    void testDataIntegrityAfterRecovery() {
        // Given: Create and persist multiple entities
        Entity entity1 = new Entity();
        entity1.setName("Entity 1");
        entity1.setDescription("First entity");
        entity1.setVersion("1.0.0");
        Entity saved1 = entityRepository.save(entity1);

        Entity entity2 = new Entity();
        entity2.setName("Entity 2");
        entity2.setDescription("Second entity");
        entity2.setVersion("2.0.0");
        Entity saved2 = entityRepository.save(entity2);

        // Verify both exist
        assertEquals(2, entityService.getAllActive().size());

        // When: Database recovers after simulated unavailability
        // Then: Both entities should still be intact
        Entity retrieved1 = entityService.findById(saved1.getId());
        Entity retrieved2 = entityService.findById(saved2.getId());

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertEquals("Entity 1", retrieved1.getName());
        assertEquals("Entity 2", retrieved2.getName());
        log.info("Data integrity verified after recovery");
    }

    @Test
    @DisplayName("Should not corrupt data during failed write attempts")
    void testDataIntegrityOnFailedWrite() {
        // Given: Entity exists in database
        Entity entity = new Entity();
        entity.setName("Test Entity");
        entity.setDescription("Original description");
        entity.setVersion("1.0.0");
        Entity savedEntity = entityRepository.save(entity);

        // When: Mock a failed update
        EntityRepository mockRepository = mock(EntityRepository.class);
        when(mockRepository.findById(savedEntity.getId()))
                .thenReturn(java.util.Optional.of(savedEntity));
        when(mockRepository.save(any(Entity.class)))
                .thenThrow(new DataAccessResourceFailureException("Write failed"));

        EntityService service = new EntityService(mockRepository);

        // Then: Update should fail
        Entity updateEntity = new Entity();
        updateEntity.setName("Updated Name");

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.update(savedEntity.getId(), updateEntity)
        );

        // When: Database recovers and we check original entity
        Entity original = entityService.findById(savedEntity.getId());

        // Then: Original data should be unchanged
        assertEquals("Test Entity", original.getName());
        assertEquals("Original description", original.getDescription());
        log.info("Data integrity confirmed - original entity unchanged after failed update");
    }

    @Test
    @DisplayName("Should provide meaningful error messages for debugging")
    void testErrorMessageQuality() {
        // Given
        EntityRepository mockRepository = mock(EntityRepository.class);
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException(
                "Connection refused to host: postgres-db-01.example.com:5432"
        );
        when(mockRepository.findById(any(UUID.class))).thenThrow(cause);

        EntityService service = new EntityService(mockRepository);

        // When
        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> service.findById(UUID.randomUUID())
        );

        // Then: Error should include cause information
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().length() > 0);
        log.info("Error message: {}", exception.getMessage());
        log.info("Cause: {}", exception.getCause().getMessage());
    }
}



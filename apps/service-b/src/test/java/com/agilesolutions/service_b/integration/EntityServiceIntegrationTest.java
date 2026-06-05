package com.agilesolutions.service_b.integration;

import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import com.agilesolutions.service_b.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Service B using Testcontainers PostgreSQL
 * 
 * Tests the complete flow from controller through service to database
 * using a real PostgreSQL container.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Service B Integration Tests with PostgreSQL")
class EntityServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_info")
            .withUsername("test_user")
            .withPassword("test_password");

    @Autowired
    private EntityService entityService;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private MockMvc mockMvc;

    private UUID testId;
    private Entity testEntity;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clear previous data
        entityRepository.deleteAll();

        // Create test entity
        testId = UUID.randomUUID();
        testEntity = Entity.builder()
                .id(testId)
                .name("Integration Test Entity")
                .description("Entity for integration testing")
                .version("1.0.0")
                .createdBy("integration-test")
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        entityRepository.save(testEntity);
    }

    @Test
    @DisplayName("Should retrieve entity from database via service")
    @Transactional
    void testFindEntityById() {
        // When
        Entity foundEntity = entityService.findActiveById(testId);

        // Then
        assertThat(foundEntity).isNotNull();
        assertThat(foundEntity.getId()).isEqualTo(testId);
        assertThat(foundEntity.getName()).isEqualTo("Integration Test Entity");
        assertThat(foundEntity.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("Should find entity by name")
    @Transactional
    void testFindEntityByName() {
        // When
        Entity foundEntity = entityService.findByName("Integration Test Entity");

        // Then
        assertThat(foundEntity).isNotNull();
        assertThat(foundEntity.getName()).isEqualTo("Integration Test Entity");
    }

    @Test
    @DisplayName("Should get entity info via REST endpoint")
    void testGetEntityInfoViaRestAPI() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/internal/info/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testId.toString())))
                .andExpect(jsonPath("$.name", is("Integration Test Entity")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    @DisplayName("Should return 404 for non-existent entity")
    void testGetNonExistentEntity() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/internal/info/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create new entity in database")
    @Transactional
    void testCreateNewEntity() {
        // Given
        UUID newId = UUID.randomUUID();
        Entity newEntity = Entity.builder()
                .id(newId)
                .name("New Entity")
                .description("Newly created entity")
                .version("1.0.0")
                .active(true)
                .build();

        // When
        Entity savedEntity = entityService.create(newEntity);

        // Then
        assertThat(savedEntity).isNotNull();
        assertThat(savedEntity.getId()).isNotNull();
        assertThat(savedEntity.getName()).isEqualTo("New Entity");
        
        // Verify persisted to database
        Entity retrievedEntity = entityService.findById(savedEntity.getId());
        assertThat(retrievedEntity).isNotNull();
    }

    @Test
    @DisplayName("Should update entity in database")
    @Transactional
    void testUpdateEntity() {
        // Given
        Entity updateData = Entity.builder()
                .name("Updated Entity Name")
                .description("Updated description")
                .version("2.0.0")
                .build();

        // When
        Entity updatedEntity = entityService.update(testId, updateData);

        // Then
        assertThat(updatedEntity.getName()).isEqualTo("Updated Entity Name");
        assertThat(updatedEntity.getDescription()).isEqualTo("Updated description");
        
        // Verify persisted to database
        Entity retrievedEntity = entityService.findById(testId);
        assertThat(retrievedEntity.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("Should deactivate entity")
    @Transactional
    void testDeleteEntity() {
        // Given
        assertThat(entityService.existsActive(testId)).isTrue();

        // When
        entityService.delete(testId);

        // Then
        assertThat(entityService.existsActive(testId)).isFalse();
    }

    @Test
    @DisplayName("Should retrieve all active entities")
    @Transactional
    void testGetAllActiveEntities() {
        // Given
        Entity entity2 = Entity.builder()
                .id(UUID.randomUUID())
                .name("Second Entity")
                .version("1.0.0")
                .active(true)
                .build();
        entityRepository.save(entity2);

        // When
        var activeEntities = entityService.getAllActive();

        // Then
        assertThat(activeEntities).hasSize(2);
        assertThat(activeEntities)
                .extracting("name")
                .containsExactlyInAnyOrder(
                        "Integration Test Entity",
                        "Second Entity"
                );
    }

    @Test
    @DisplayName("Should timestamp fields on creation")
    @Transactional
    void testTimestampOnCreation() {
        // Given
        Entity newEntity = Entity.builder()
                .name("Timestamp Test Entity")
                .version("1.0.0")
                .build();

        // When
        Entity savedEntity = entityService.create(newEntity);

        // Then
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should update timestamp on update")
    @Transactional
    void testTimestampOnUpdate() {
        // Given
        LocalDateTime originalUpdatedAt = testEntity.getUpdatedAt();

        // When
        Entity updateData = Entity.builder()
                .name("Updated Name")
                .build();
        Entity updated = entityService.update(testId, updateData);

        // Then
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should handle concurrent entity retrievals")
    void testConcurrentEntityRetrieval() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Entity foundEntity = entityService.findActiveById(testId);
                assertThat(foundEntity).isNotNull();
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then - all threads completed successfully
        assertThat(entityService.findActiveById(testId)).isNotNull();
    }
}


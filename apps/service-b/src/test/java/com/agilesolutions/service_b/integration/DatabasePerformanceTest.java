package com.agilesolutions.service_b.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database performance with Testcontainers
 * 
 * Tests query performance under various conditions including slow queries
 */
@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@Import(EntityService.class)
@Slf4j
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {

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
        // Clear any existing data
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Should retrieve entity within acceptable latency (< 100ms)")
    void testQueryPerformance() {
        // Given
        Entity entity = new Entity();
        entity.setName("Performance Test Entity");
        entity.setDescription("Test entity for performance measurement");
        entity.setVersion("1.0.0");
        Entity savedEntity = entityRepository.save(entity);

        // When
        long startTime = System.currentTimeMillis();
        Entity retrieved = entityService.findById(savedEntity.getId());
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertNotNull(retrieved);
        assertEquals(savedEntity.getId(), retrieved.getId());
        assertTrue(duration < 100, "Query should complete within 100ms, took: " + duration + "ms");
        log.info("Query completed in {}ms", duration);
    }

    @Test
    @DisplayName("Should handle bulk insertion without performance degradation")
    void testBulkInsertPerformance() {
        // Given: Insert 1000 entities
        log.info("Starting bulk insert test with 1000 entities");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            Entity entity = new Entity();
            entity.setName("Entity " + i);
            entity.setDescription("Bulk insert test entity " + i);
            entity.setVersion("1.0.0");
            entityRepository.save(entity);
        }

        long insertTime = System.currentTimeMillis() - startTime;
        log.info("Bulk insert completed in {}ms for 1000 entities", insertTime);

        // When
        startTime = System.currentTimeMillis();
        List<Entity> allEntities = entityService.getAllActive();
        long queryTime = System.currentTimeMillis() - startTime;
        log.info("Query all completed in {}ms", queryTime);

        // Then
        assertEquals(1000, allEntities.size());
        assertTrue(queryTime < 500, "Bulk query should complete within 500ms, took: " + queryTime + "ms");
    }

    @Test
    @DisplayName("Should handle indexed query performance (name lookup)")
    void testIndexedQueryPerformance() {
        // Given: Create entity and wait for indexes
        Entity entity = new Entity();
        entity.setName("Indexed Query Test");
        entity.setDescription("Entity for indexed query test");
        entity.setVersion("1.0.0");
        entityRepository.save(entity);

        // When
        long startTime = System.currentTimeMillis();
        Entity retrieved = entityService.findByName("Indexed Query Test");
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(retrieved);
        assertEquals("Indexed Query Test", retrieved.getName());
        assertTrue(duration < 100, "Indexed query should complete within 100ms, took: " + duration + "ms");
        log.info("Indexed query completed in {}ms", duration);
    }

    @Test
    @DisplayName("Should efficiently find active entities by status")
    void testActiveEntityQueryPerformance() {
        // Given
        for (int i = 0; i < 100; i++) {
            Entity entity = new Entity();
            entity.setName("Active Entity " + i);
            entity.setDescription("Active entity for performance test");
            entity.setVersion("1.0.0");
            entity.setActive(true);
            entityRepository.save(entity);
        }

        // And some inactive entities
        for (int i = 0; i < 50; i++) {
            Entity entity = new Entity();
            entity.setName("Inactive Entity " + i);
            entity.setDescription("Inactive entity for performance test");
            entity.setVersion("1.0.0");
            entity.setActive(false);
            entityRepository.save(entity);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<Entity> activeEntities = entityService.getAllActive();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(100, activeEntities.size());
        assertTrue(duration < 200, "Active entity query should complete within 200ms, took: " + duration + "ms");
        log.info("Active entity query completed in {}ms", duration);
    }

    @Test
    @DisplayName("Should maintain performance with large result sets")
    void testLargeResultSetPerformance() {
        // Given: Create many entities
        log.info("Creating 500 entities for large result set test");
        for (int i = 0; i < 500; i++) {
            Entity entity = new Entity();
            entity.setName("Large Result Entity " + i);
            entity.setDescription("Entity " + i + " for large result set test");
            entity.setVersion("1.0.0");
            entityRepository.save(entity);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<Entity> entities = entityService.getAllActive();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertEquals(500, entities.size());
        assertTrue(duration < 1000, "Large result query should complete within 1000ms, took: " + duration + "ms");
        log.info("Large result set query completed in {}ms for {} entities", duration, entities.size());
    }

    @Test
    @DisplayName("Should verify index effectiveness for unique name lookups")
    void testUniqueNameIndexPerformance() {
        // Given: Create entity with unique name
        Entity entity = new Entity();
        entity.setName("UniquePerformanceTestEntity");
        entity.setDescription("Test unique index performance");
        entity.setVersion("1.0.0");
        entityRepository.save(entity);

        // When: Multiple lookups should all be fast
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            Entity retrieved = entityService.findByName("UniquePerformanceTestEntity");
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertNotNull(retrieved);
            assertTrue(duration < 50, "Repeated indexed lookup should be very fast");
        }
        
        log.info("All 10 lookups completed successfully with index");
    }

    @Test
    @DisplayName("Should handle concurrent read performance")
    void testConcurrentReadPerformance() throws InterruptedException {
        // Given: Create test entity
        Entity entity = new Entity();
        entity.setName("Concurrent Read Test");
        entity.setDescription("Entity for concurrent read performance test");
        entity.setVersion("1.0.0");
        Entity savedEntity = entityRepository.save(entity);

        // When: 50 concurrent reads
        Thread[] threads = new Thread[50];
        long[] durations = new long[50];

        for (int i = 0; i < 50; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                Entity retrieved = entityService.findById(savedEntity.getId());
                durations[index] = System.currentTimeMillis() - startTime;
                assertNotNull(retrieved);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Then: All reads should be fast
        long avgDuration = 0;
        for (long duration : durations) {
            avgDuration += duration;
        }
        avgDuration /= durations.length;

        log.info("Average duration for concurrent reads: {}ms", avgDuration);
        assertTrue(avgDuration < 100, "Average concurrent read should be under 100ms");
    }
}



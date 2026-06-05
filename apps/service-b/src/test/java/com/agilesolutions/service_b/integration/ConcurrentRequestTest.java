package com.agilesolutions.service_b.integration;

import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import com.agilesolutions.service_b.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for concurrent request handling
 * 
 * Tests that Entity service can handle 100+ concurrent requests without
 * data corruption, race conditions, or performance degradation
 */
@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@Import(EntityService.class)
@Slf4j
@DisplayName("Concurrent Request Tests")
class ConcurrentRequestTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityService entityService;

    private static final int CONCURRENT_THREADS = 100;
    private static final int ENTITIES_TO_CREATE = 10;

    @BeforeEach
    void setUp() {
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle 100 concurrent read requests without data corruption")
    void testConcurrentReads() throws InterruptedException {
        // Given: Create test entities
        List<Entity> createdEntities = new ArrayList<>();
        for (int i = 0; i < ENTITIES_TO_CREATE; i++) {
            Entity entity = new Entity();
            entity.setName("Concurrent Test Entity " + i);
            entity.setDescription("Entity for concurrent read test " + i);
            entity.setVersion("1.0.0");
            createdEntities.add(entityRepository.save(entity));
        }

        log.info("Created {} entities for concurrent read test", ENTITIES_TO_CREATE);

        // When: Execute 100 concurrent read requests
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int entityIndex = i % ENTITIES_TO_CREATE;
            final UUID entityId = createdEntities.get(entityIndex).getId();

            executor.submit(() -> {
                try {
                    Entity retrieved = entityService.findById(entityId);
                    assertNotNull(retrieved);
                    assertEquals(entityId, retrieved.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error during concurrent read", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within 30 seconds");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent reads");
        assertEquals(CONCURRENT_THREADS, successCount.get(), "All concurrent reads should succeed");

        log.info("Successfully completed {} concurrent read requests", CONCURRENT_THREADS);
    }

    @Test
    @DisplayName("Should maintain data consistency with concurrent writes")
    void testConcurrentWrites() throws InterruptedException {
        // When: Execute 100 concurrent write requests
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        List<UUID> createdIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    Entity entity = new Entity();
                    entity.setName("Concurrent Write Entity " + index);
                    entity.setDescription("Concurrent write test entity " + index);
                    entity.setVersion("1.0.0");

                    Entity created = entityService.create(entity);
                    createdIds.add(created.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error during concurrent write", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within 60 seconds");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent writes");
        assertEquals(CONCURRENT_THREADS, createdIds.size(), "All entities should be created");

        // Verify all entities exist with correct data
        List<Entity> allEntities = entityService.getAllActive();
        assertEquals(CONCURRENT_THREADS, allEntities.size(), "All created entities should be active");

        log.info("Successfully created {} entities concurrently", CONCURRENT_THREADS);
    }

    @Test
    @DisplayName("Should handle mixed read/write operations concurrently")
    void testConcurrentMixedOperations() throws InterruptedException {
        // Given: Create initial entities
        List<Entity> initialEntities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Entity entity = new Entity();
            entity.setName("Mixed Op Entity " + i);
            entity.setDescription("Initial entity for mixed operations");
            entity.setVersion("1.0.0");
            initialEntities.add(entityRepository.save(entity));
        }

        // When: Execute 100 concurrent mixed operations (50% reads, 50% writes)
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // Read operation
                        UUID readId = initialEntities.get(index % initialEntities.size()).getId();
                        Entity retrieved = entityService.findById(readId);
                        assertNotNull(retrieved);
                        readCount.incrementAndGet();
                    } else {
                        // Write operation
                        Entity entity = new Entity();
                        entity.setName("Mixed Write Entity " + index);
                        entity.setDescription("Concurrent write in mixed test");
                        entity.setVersion("1.0.0");
                        Entity created = entityService.create(entity);
                        assertNotNull(created.getId());
                        writeCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error during mixed operation", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during mixed operations");
        assertEquals(CONCURRENT_THREADS, readCount.get() + writeCount.get(), "All operations should succeed");

        log.info("Successfully completed {} concurrent mixed operations ({} reads, {} writes)",
                CONCURRENT_THREADS, readCount.get(), writeCount.get());
    }

    @Test
    @DisplayName("Should prevent race conditions on entity updates")
    void testConcurrentUpdateRaceCondition() throws InterruptedException {
        // Given: Create a single entity
        Entity entity = new Entity();
        entity.setName("Race Condition Test");
        entity.setDescription("Original description");
        entity.setVersion("1.0.0");
        Entity savedEntity = entityRepository.save(entity);

        // When: Execute 50 concurrent update attempts
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(50);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 50; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    Entity updateEntity = new Entity();
                    updateEntity.setDescription("Updated by thread " + index);
                    updateEntity.setVersion("2.0.0");

                    Entity updated = entityService.update(savedEntity.getId(), updateEntity);
                    assertNotNull(updated);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error during concurrent update", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All updates should complete");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur");
        assertEquals(50, successCount.get(), "All updates should succeed");

        // Verify final state
        Entity finalEntity = entityService.findById(savedEntity.getId());
        assertNotNull(finalEntity.getDescription());
        assertEquals("2.0.0", finalEntity.getVersion());

        log.info("Race condition test passed - {} concurrent updates handled correctly", 50);
    }

    @Test
    @DisplayName("Should handle concurrent queries by name")
    void testConcurrentNameQueries() throws InterruptedException {
        // Given: Create entities with unique names
        Map<String, UUID> entityMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 20; i++) {
            Entity entity = new Entity();
            entity.setName("Unique Name " + i);
            entity.setDescription("Entity with unique name " + i);
            entity.setVersion("1.0.0");
            Entity saved = entityRepository.save(entity);
            entityMap.put("Unique Name " + i, saved.getId());
        }

        // When: Execute 100 concurrent name-based queries (5 queries each for each name)
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    String name = "Unique Name " + (index % 20);
                    Entity retrieved = entityService.findByName(name);
                    assertNotNull(retrieved);
                    assertEquals(entityMap.get(name), retrieved.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error during concurrent name query", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent queries");
        assertEquals(CONCURRENT_THREADS, successCount.get(), "All queries should succeed");

        log.info("Successfully completed {} concurrent name-based queries", CONCURRENT_THREADS);
    }

    @Test
    @DisplayName("Should maintain transaction boundaries under concurrent load")
    void testConcurrentTransactionBoundaries() throws InterruptedException {
        // When: Execute 100 concurrent create operations
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        List<UUID> createdIds = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;

            executor.submit(() -> {
                try {
                    Entity entity = new Entity();
                    entity.setName("Transaction Test " + index);
                    entity.setDescription("Entity created in transaction " + index);
                    entity.setVersion("1.0.0");

                    Entity created = entityService.create(entity);
                    createdIds.add(created.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Error in concurrent transaction", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all tasks to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All transactions should complete");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur");
        assertEquals(CONCURRENT_THREADS, createdIds.size(), "All entities should be created");

        // Verify all entities are distinct
        Set<UUID> uniqueIds = new HashSet<>(createdIds);
        assertEquals(CONCURRENT_THREADS, uniqueIds.size(), "All created entities should have unique IDs");

        log.info("Transaction boundary test passed - {} unique entities created successfully", CONCURRENT_THREADS);
    }

    @Test
    @DisplayName("Should not degrade performance with concurrent load")
    void testConcurrentPerformanceUnderLoad() throws InterruptedException {
        // Given: Create test entities
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Entity entity = new Entity();
            entity.setName("Load Test Entity " + i);
            entity.setDescription("Entity for load testing");
            entity.setVersion("1.0.0");
            entities.add(entityRepository.save(entity));
        }

        // When: Execute 100 concurrent reads and measure time
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        long[] durations = new long[CONCURRENT_THREADS];

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int index = i;
            final Entity entity = entities.get(i % entities.size());

            executor.submit(() -> {
                long opStart = System.currentTimeMillis();
                try {
                    Entity retrieved = entityService.findById(entity.getId());
                    assertNotNull(retrieved);
                } finally {
                    durations[index] = System.currentTimeMillis() - opStart;
                    latch.countDown();
                }
            });
        }

        // Then: Wait for completion and analyze performance
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        assertTrue(completed, "All concurrent operations should complete");

        // Calculate statistics
        long maxDuration = 0;
        long avgDuration = 0;
        for (long duration : durations) {
            maxDuration = Math.max(maxDuration, duration);
            avgDuration += duration;
        }
        avgDuration /= CONCURRENT_THREADS;

        // Performance assertions
        assertTrue(maxDuration < 500, "Max operation duration should be under 500ms, was: " + maxDuration);
        assertTrue(avgDuration < 200, "Average operation duration should be under 200ms, was: " + avgDuration);

        log.info("Concurrent performance test: Total={}ms, Max={}ms, Avg={}ms",
                totalTime, maxDuration, avgDuration);
    }
}



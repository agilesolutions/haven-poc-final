package com.agilesolutions.service_b.service;

import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service class for entity business logic operations.
 * 
 * Provides methods for retrieving, creating, updating, and managing entities
 * with proper error handling and logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EntityService {

    private final EntityRepository entityRepository;

    /**
     * Find entity by ID
     * 
     * @param id the entity UUID
     * @return the entity if found
     * @throws ResponseStatusException with 404 if entity not found
     */
    public Entity findById(UUID id) {
        log.debug("Finding entity with id: {}", id);
        
        return entityRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Entity not found with id: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Entity not found with id: " + id
                    );
                });
    }

    /**
     * Find active entity by ID
     * 
     * @param id the entity UUID
     * @return the active entity if found
     * @throws ResponseStatusException with 404 if entity not found
     */
    public Entity findActiveById(UUID id) {
        log.debug("Finding active entity with id: {}", id);
        
        return entityRepository.findByIdAndActive(id, true)
                .orElseThrow(() -> {
                    log.warn("Active entity not found with id: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Active entity not found with id: " + id
                    );
                });
    }

    /**
     * Find entity by name
     * 
     * @param name the entity name
     * @return the entity if found
     * @throws ResponseStatusException with 404 if entity not found
     */
    public Entity findByName(String name) {
        log.debug("Finding entity with name: {}", name);
        
        return entityRepository.findActiveByName(name)
                .orElseThrow(() -> {
                    log.warn("Entity not found with name: {}", name);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Entity not found with name: " + name
                    );
                });
    }

    /**
     * Get all active entities
     * 
     * @return list of active entities
     */
    public List<Entity> getAllActive() {
        log.debug("Retrieving all active entities");
        return entityRepository.findAllActive();
    }

    /**
     * Create a new entity
     * 
     * @param entity the entity to create
     * @return the created entity
     */
    @Transactional
    public Entity create(Entity entity) {
        log.info("Creating new entity with name: {}", entity.getName());
        
        if (entity.getVersion() == null || entity.getVersion().isEmpty()) {
            entity.setVersion("1.0.0");
        }
        
        entity.setActive(true);
        Entity saved = entityRepository.save(entity);
        
        log.info("Entity created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Update an existing entity
     * 
     * @param id the entity ID
     * @param entity the updated entity data
     * @return the updated entity
     * @throws ResponseStatusException with 404 if entity not found
     */
    @Transactional
    public Entity update(UUID id, Entity entity) {
        log.info("Updating entity with id: {}", id);
        
        Entity existing = findById(id);
        
        if (entity.getName() != null && !entity.getName().isEmpty()) {
            existing.setName(entity.getName());
        }
        if (entity.getDescription() != null) {
            existing.setDescription(entity.getDescription());
        }
        if (entity.getVersion() != null && !entity.getVersion().isEmpty()) {
            existing.setVersion(entity.getVersion());
        }
        if (entity.getUpdatedBy() != null) {
            existing.setUpdatedBy(entity.getUpdatedBy());
        }
        
        Entity updated = entityRepository.save(existing);
        log.info("Entity updated successfully with id: {}", updated.getId());
        return updated;
    }

    /**
     * Delete (deactivate) an entity
     * 
     * @param id the entity ID
     * @throws ResponseStatusException with 404 if entity not found
     */
    @Transactional
    public void delete(UUID id) {
        log.info("Deactivating entity with id: {}", id);
        
        Entity entity = findById(id);
        entity.setActive(false);
        entityRepository.save(entity);
        
        log.info("Entity deactivated successfully with id: {}", id);
    }

    /**
     * Check if entity exists
     * 
     * @param id the entity ID
     * @return true if entity exists and is active, false otherwise
     */
    public boolean existsActive(UUID id) {
        return entityRepository.existsActiveById(id);
    }

    /**
     * Check if entity exists (including inactive)
     * 
     * @param id the entity ID
     * @return true if entity exists, false otherwise
     */
    public boolean exists(UUID id) {
        return entityRepository.existsById(id);
    }
}


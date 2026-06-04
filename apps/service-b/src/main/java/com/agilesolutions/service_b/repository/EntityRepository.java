package com.agilesolutions.service_b.repository;

import com.agilesolutions.service_b.model.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Entity persistence operations.
 * 
 * Extends JpaRepository to provide CRUD operations and custom query methods
 * for retrieving entity information from the database.
 */
@Repository
public interface EntityRepository extends JpaRepository<Entity, UUID> {

    /**
     * Find entity by name
     * @param name the entity name
     * @return Optional containing the entity if found
     */
    Optional<Entity> findByName(String name);

    /**
     * Find entity by id and active status
     * @param id the entity id
     * @param active the active status
     * @return Optional containing the entity if found
     */
    Optional<Entity> findByIdAndActive(UUID id, Boolean active);

    /**
     * Find active entities by name
     * @param name the entity name
     * @return Optional containing the active entity if found
     */
    @Query("SELECT e FROM Entity e WHERE e.name = :name AND e.active = true")
    Optional<Entity> findActiveByName(@Param("name") String name);

    /**
     * Find all active entities
     * @return list of active entities
     */
    @Query("SELECT e FROM Entity e WHERE e.active = true ORDER BY e.createdAt DESC")
    List<Entity> findAllActive();

    /**
     * Check if entity exists and is active
     * @param id the entity id
     * @return true if entity exists and is active, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entity e WHERE e.id = :id AND e.active = true")
    boolean existsActiveById(@Param("id") UUID id);

    /**
     * Find entity by id regardless of active status
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    @Query("SELECT e FROM Entity e WHERE e.id = :id")
    Optional<Entity> findByIdIncludingInactive(@Param("id") UUID id);
}


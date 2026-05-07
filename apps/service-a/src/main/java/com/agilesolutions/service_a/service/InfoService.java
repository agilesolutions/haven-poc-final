package com.agilesolutions.service_a.service;

import com.agilesolutions.service_a.model.EntityInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Service for orchestrating entity information retrieval from Service B.
 * 
 * Acts as a gateway service that coordinates calling Service B
 * to fetch entity information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfoService {

    private final EntityClient entityClient;

    /**
     * Retrieve entity information by ID
     * 
     * @param entityId the entity UUID
     * @return EntityInfo containing the entity details
     * @throws RestClientException if Entity Not Found (404) or Server Error (503)
     */
    public EntityInfo getEntityInfo(UUID entityId) {
        log.info("Service A: Getting entity info for id: {}", entityId);
        
        try {
            // Call Service B's internal API with OAuth2 authentication
            EntityInfo entityInfo = entityClient.getEntityInfo(entityId);
            
            log.info("Service A: Successfully retrieved entity info from Service B for id: {}", entityId);
            return entityInfo;
            
        } catch (RestClientException e) {
            log.error("Service A: Failed to retrieve entity info from Service B for id: {}", entityId, e);
            throw e;
        }
    }

    /**
     * Retrieve entity information by name
     * 
     * @param entityName the entity name
     * @return EntityInfo containing the entity details
     * @throws RestClientException if Entity Not Found (404) or Server Error (503)
     */
    public EntityInfo getEntityInfoByName(String entityName) {
        log.info("Service A: Getting entity info for name: {}", entityName);
        
        try {
            // Call Service B's internal API with OAuth2 authentication
            EntityInfo entityInfo = entityClient.getEntityInfoByName(entityName);
            
            log.info("Service A: Successfully retrieved entity info from Service B for name: {}", entityName);
            return entityInfo;
            
        } catch (RestClientException e) {
            log.error("Service A: Failed to retrieve entity info from Service B for name: {}", entityName, e);
            throw e;
        }
    }

    /**
     * Check if entity exists by calling Service B
     * 
     * @param entityId the entity UUID
     * @return true if entity exists and is accessible, false otherwise
     */
    public boolean entityExists(UUID entityId) {
        log.debug("Service A: Checking if entity exists with id: {}", entityId);
        
        try {
            entityClient.getEntityInfo(entityId);
            return true;
        } catch (RestClientException e) {
            log.debug("Service A: Entity does not exist or is not accessible: {}", entityId);
            return false;
        }
    }
}


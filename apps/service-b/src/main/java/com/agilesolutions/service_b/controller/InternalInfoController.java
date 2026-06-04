package com.agilesolutions.service_b.controller;

import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.model.EntityInfo;
import com.agilesolutions.service_b.service.EntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal REST Controller for Entity Information API
 * 
 * Provides endpoint for retrieving entity information.
 * This endpoint is protected by OAuth2 and should only be called by Service A.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SCOPE_service-a')")
public class InternalInfoController {

    private final EntityService entityService;

    /**
     * Get entity information by ID
     * 
     * @param id the entity UUID as path variable
     * @return ResponseEntity with entity information wrapped in EntityInfo or 404 if not found
     */
    @GetMapping("/info/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EntityInfo> getInternalInfo(@PathVariable String id) {
        log.debug("Received request for entity info with id: {}", id);
        
        try {
            UUID entityId = UUID.fromString(id);
            Entity entity = entityService.findActiveById(entityId);
            
            EntityInfo info = EntityInfo.builder()
                    .id(entity.getId().toString())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .version(entity.getVersion())
                    .build();
            
            log.debug("Successfully retrieved entity info for id: {}", id);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get entity information by name (alternative endpoint)
     * 
     * @param name the entity name as query parameter
     * @return ResponseEntity with entity information wrapped in EntityInfo or 404 if not found
     */
    @GetMapping("/info")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<EntityInfo> getInternalInfoByName(@RequestParam String name) {
        log.debug("Received request for entity info with name: {}", name);
        
        Entity entity = entityService.findByName(name);
        
        EntityInfo info = EntityInfo.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .description(entity.getDescription())
                .version(entity.getVersion())
                .build();
        
        log.debug("Successfully retrieved entity info for name: {}", name);
        return ResponseEntity.ok(info);
    }
}



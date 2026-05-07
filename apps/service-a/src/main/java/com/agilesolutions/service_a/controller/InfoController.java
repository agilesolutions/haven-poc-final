package com.agilesolutions.service_a.controller;

import com.agilesolutions.service_a.model.EntityInfo;
import com.agilesolutions.service_a.service.InfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * REST Controller for Entity Information API (Public Gateway)
 * 
 * Provides external API endpoint for retrieving entity information.
 * Acts as a gateway forwarding requests to Service B through InfoService.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InfoController {

    private final InfoService infoService;

    /**
     * Get entity information by ID
     * 
     * @param id the entity UUID
     * @return ResponseEntity with entity information or appropriate error status
     */
    @GetMapping("/info/{id}")
    public ResponseEntity<EntityInfo> getInfo(@PathVariable String id) {
        log.debug("Received request for entity info with id: {}", id);
        
        try {
            // Validate UUID format
            UUID entityId = UUID.fromString(id);
            
            // Call InfoService to retrieve entity information from Service B
            EntityInfo info = infoService.getEntityInfo(entityId);
            
            log.debug("Successfully retrieved entity info for id: {}", id);
            return ResponseEntity.ok(info);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for id: {}", id);
            return ResponseEntity.badRequest().build();
            
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Entity not found for id: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Authentication failed when calling Service B for id: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("Access forbidden when calling Service B for id: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            
        } catch (RestClientException e) {
            log.error("Service B is unavailable or connection error for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving entity info for id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get entity information by name
     * 
     * @param name the entity name
     * @return ResponseEntity with entity information or appropriate error status
     */
    @GetMapping("/info")
    public ResponseEntity<EntityInfo> getInfoByName(@RequestParam(name = "name") String name) {
        log.debug("Received request for entity info with name: {}", name);
        
        if (name == null || name.trim().isEmpty()) {
            log.error("Entity name parameter is empty");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Call InfoService to retrieve entity information from Service B
            EntityInfo info = infoService.getEntityInfoByName(name);
            
            log.debug("Successfully retrieved entity info for name: {}", name);
            return ResponseEntity.ok(info);
            
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Entity not found for name: {}", name);
            return ResponseEntity.notFound().build();
            
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Authentication failed when calling Service B for name: {}", name);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (RestClientException e) {
            log.error("Service B is unavailable or connection error for name: {}", name, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving entity info for name: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


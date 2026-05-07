package com.agilesolutions.service_a.service;

import com.agilesolutions.service_a.model.EntityInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * HTTP Client for Service A to call Service B's internal API
 * 
 * Handles OAuth2 Client Credentials flow for service-to-service authentication
 * and retrieves entity information from Service B.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityClient {

    @Value("${service.b.url:http://localhost:8081}")
    private String serviceBUrl;

    @Value("${service.b.timeout-ms:5000}")
    private long timeoutMs;

    @Value("${service.b.max-retries:3}")
    private int maxRetries;

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    /**
     * Get entity information from Service B by entity ID
     * 
     * @param entityId the entity UUID
     * @return EntityInfo containing the entity details
     * @throws RestClientException if the request fails
     */
    public EntityInfo getEntityInfo(UUID entityId) {
        log.debug("Fetching entity info from Service B for id: {}", entityId);
        
        String url = String.format("%s/api/internal/info/%s", serviceBUrl, entityId);
        
        return executeWithRetry(url, entityId.toString());
    }

    /**
     * Get entity information from Service B by entity name
     * 
     * @param entityName the entity name
     * @return EntityInfo containing the entity details
     * @throws RestClientException if the request fails
     */
    public EntityInfo getEntityInfoByName(String entityName) {
        log.debug("Fetching entity info from Service B for name: {}", entityName);
        
        String url = String.format("%s/api/internal/info?name=%s", serviceBUrl, entityName);
        
        return executeWithRetry(url, entityName);
    }

    /**
     * Execute HTTP request with retry logic
     * 
     * @param url the target URL
     * @param identifier the entity identifier (for logging)
     * @return EntityInfo from Service B
     */
    private EntityInfo executeWithRetry(String url, String identifier) {
        RestClientException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Attempt {}/{} to fetch from Service B: {}", attempt, maxRetries, url);
                
                String token = getOAuth2Token();
                HttpHeaders headers = createHeaders(token);
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
                
                ResponseEntity<EntityInfo> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        EntityInfo.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Successfully fetched entity info for: {}", identifier);
                    return response.getBody();
                }
                
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Entity not found in Service B for: {}", identifier);
                throw e;
            } catch (RestClientException e) {
                log.warn("Attempt {}/{} failed to fetch from Service B: {}", attempt, maxRetries, e.getMessage());
                lastException = e;
                
                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff: 100ms * 2^(attempt-1)
                        long backoffMs = 100L * (long) Math.pow(2, attempt - 1);
                        backoffMs = Math.min(backoffMs, 2000); // Cap at 2 seconds
                        log.debug("Waiting {} ms before retry", backoffMs);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted during retry backoff", ie);
                        throw new RuntimeException("Interrupted while retrying", ie);
                    }
                }
            }
        }
        
        log.error("Failed to fetch entity info for {} after {} attempts", identifier, maxRetries);
        throw new RuntimeException(
                String.format("Failed to fetch entity info for %s after %d attempts", identifier, maxRetries),
                lastException
        );
    }

    /**
     * Acquire OAuth2 token from Keycloak using Client Credentials flow
     * 
     * @return OAuth2 access token
     */
    private String getOAuth2Token() {
        try {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak")
                    .principal("service-a")
                    .build();
            
            var authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                throw new RuntimeException("Failed to obtain OAuth2 token");
            }
            
            String token = authorizedClient.getAccessToken().getTokenValue();
            log.debug("Successfully obtained OAuth2 token");
            return token;
            
        } catch (Exception e) {
            log.error("Failed to obtain OAuth2 token from Keycloak", e);
            throw new RuntimeException("Failed to obtain OAuth2 token", e);
        }
    }

    /**
     * Create HTTP headers with OAuth2 bearer token
     * 
     * @param token the OAuth2 access token
     * @return HttpHeaders with Authorization header
     */
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "Service-A/1.0.0");
        return headers;
    }
}


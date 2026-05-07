package com.agilesolutions.service_a.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * OAuth2 Client Configuration for Service A
 * 
 * Configures OAuth2 Client Credentials Flow to authenticate with Keycloak
 * and obtain JWT tokens for service-to-service communication with Service B.
 * 
 * The OAuth2 client settings are defined in application.yaml:
 * - client-id: service-a
 * - client-secret: (from Keycloak)
 * - authorization-grant-type: client_credentials
 * - token-uri: (Keycloak token endpoint)
 */
@Configuration
@Slf4j
public class OAuth2ClientConfig {

    /**
     * OAuth2AuthorizedClientManager bean for managing client credentials flow
     * 
     * This manager handles:
     * - Token acquisition from Keycloak using client credentials
     * - Token caching and refresh
     * - Thread-safe token management
     * 
     * @param clientRegistrationRepository Repository of OAuth2 client registrations
     * @param authorizedClientRepository Repository of authorized clients and tokens
     * @return Configured OAuth2AuthorizedClientManager
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        
        log.debug("Configuring OAuth2AuthorizedClientManager for Client Credentials flow");
        
        // Configure provider for client credentials flow with refresh token support
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()  // Client Credentials (service-to-service)
                        .refreshToken()       // Support token refresh if needed
                        .build();
        
        DefaultOAuth2AuthorizedClientManager clientManager = 
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository);
        
        clientManager.setAuthorizedClientProvider(authorizedClientProvider);
        
        log.debug("OAuth2AuthorizedClientManager configured successfully");
        return clientManager;
    }
}


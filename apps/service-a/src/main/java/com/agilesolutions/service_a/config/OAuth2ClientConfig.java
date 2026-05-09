package com.agilesolutions.service_a.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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
     /**
      * RestTemplate bean with timeout configuration
      * 
      * Configures connection and read timeouts to prevent indefinite hangs
      * when Service B is unavailable or slow.
      * 
      * @param builder RestTemplateBuilder for easy configuration
      * @return RestTemplate with timeout settings
      */
     @Bean
     public RestTemplate restTemplate(RestTemplateBuilder builder) {
         log.debug("Configuring RestTemplate with timeout settings");
         
         ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(
                 new SimpleClientHttpRequestFactory() {{
                     setConnectTimeout((int) Duration.ofSeconds(5).toMillis());  // 5 second connection timeout
                     setReadTimeout((int) Duration.ofSeconds(5).toMillis());     // 5 second read timeout
                 }}
         );
         
         return builder
                 .requestFactory(() -> factory)
                 .setConnectTimeout(Duration.ofSeconds(5))
                 .setReadTimeout(Duration.ofSeconds(5))
                 .build();
     }

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


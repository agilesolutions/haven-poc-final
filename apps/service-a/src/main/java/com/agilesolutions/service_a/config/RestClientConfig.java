package com.agilesolutions.service_a.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient Configuration for Service A
 *
 * Configures the modern RestClient with timeout settings for secure
 * service-to-service communication with Service B.
 *
 * RestClient is the modern replacement for RestTemplate in Spring Boot 4.x,
 * providing a fluent, immutable API for HTTP operations.
 */
@Configuration
@Slf4j
public class RestClientConfig {

    /**
     * RestClient bean with timeout configuration
     *
     * Configures connection and read timeouts to prevent indefinite hangs
     * when Service B is unavailable or slow. The RestClient will be used by
     * EntityClient for all service-to-service HTTP calls.
     *
     * @param builder RestClient.Builder for easy configuration
     * @return Configured RestClient instance
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        log.debug("Configuring RestClient with timeout settings");

        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
                    setReadTimeout((int) Duration.ofSeconds(5).toMillis());
                }}
        );

        return builder
                .requestFactory(factory)
                .build();
    }
}


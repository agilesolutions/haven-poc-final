package com.agilesolutions.service_a.config;

/**
 * DEPRECATED: This configuration class has been replaced by RestClientConfig
 *
 * RestTemplate (legacy) has been replaced with RestClient (Spring Boot 4.x modern API).
 *
 * The RestClient bean is now configured in RestClientConfig.java with the same
 * timeout settings and is injected into EntityClient for all HTTP operations.
 *
 * This file is kept for reference only and will be removed in a future cleanup.
 *
 * @deprecated Use RestClientConfig instead
 */
@Deprecated(since = "7.1.0", forRemoval = true)
public class RestTemplateConfig {
    // Deprecated - use RestClientConfig instead
}


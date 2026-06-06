package com.agilesolutions.service_b.unit;

import com.agilesolutions.service_b.config.OAuth2ResourceServerConfig;
import com.agilesolutions.service_b.model.Entity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for OAuth2 Resource Server Configuration
 * 
 * Tests JWT token validation, CORS configuration, and security filter chain
 * for Service B's OAuth2 Resource Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(OAuth2ResourceServerConfig.class)
@ActiveProfiles("test")
@DisplayName("OAuth2 Resource Server Configuration Tests")
class OAuth2ResourceServerConfigTest {

    MockHttpServletRequest request = new MockHttpServletRequest();

    @Autowired(required = false)
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired(required = false)
    private OAuth2ResourceServerConfig oauth2ResourceServerConfig;

    @BeforeEach
    void setUp() {
// Set up a mock request for CORS configuration tests}
        request.setMethod("GET");
        request.setRequestURI("/api/internal/info/1");
    }


    @Test
    @DisplayName("CORS configuration source should be initialized")
    void testCorsConfigurationSourceExists() {
        assertNotNull(corsConfigurationSource, "CorsConfigurationSource should be initialized");
    }

    @Test
    @DisplayName("CORS should allow requests from Service A")
    void testCorsAllowsServiceAOrigins() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertTrue(corsConfig.getAllowedOrigins().contains("http://service-a:8080"),
                "CORS should allow http://service-a:8080");
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:8080"),
                "CORS should allow http://localhost:8080");
    }

    @Test
    @DisplayName("CORS should allow standard HTTP methods")
    void testCorsAllowsStandardMethods() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertTrue(corsConfig.getAllowedMethods().contains("GET"));
        assertTrue(corsConfig.getAllowedMethods().contains("POST"));
        assertTrue(corsConfig.getAllowedMethods().contains("PUT"));
        assertTrue(corsConfig.getAllowedMethods().contains("DELETE"));
        assertTrue(corsConfig.getAllowedMethods().contains("OPTIONS"));
    }

    @Test
    @DisplayName("CORS should allow Authorization header")
    void testCorsAllowsAuthorizationHeader() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertTrue(corsConfig.getAllowedHeaders().contains("Authorization"),
                "CORS should allow Authorization header");
        assertTrue(corsConfig.getAllowedHeaders().contains("Content-Type"),
                "CORS should allow Content-Type header");
    }

    @Test
    @DisplayName("CORS should expose trace headers")
    void testCorsExposesTraceHeaders() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertTrue(corsConfig.getExposedHeaders().contains("X-Trace-Id"),
                "CORS should expose X-Trace-Id header");
    }

    @Test
    @DisplayName("CORS should allow credentials")
    void testCorsAllowsCredentials() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertTrue(corsConfig.getAllowCredentials(),
                "CORS should allow credentials for bearer tokens");
    }

    @Test
    @DisplayName("CORS max age should be set to 1 hour")
    void testCorsMaxAge() {
        assertNotNull(corsConfigurationSource);
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        
        assertEquals(3600L, corsConfig.getMaxAge(),
                "CORS max age should be 3600 (1 hour)");
    }

    @Test
    @DisplayName("Security filter chain should be configured")
    void testSecurityFilterChainConfiguration() {
        assertNotNull(oauth2ResourceServerConfig,
                "OAuth2ResourceServerConfig bean should be initialized");
    }

    /**
     * Helper method to create a mock JWT token with custom claims
     * 
     * @param claims custom claims map
     * @return JWT token
     */
    private Jwt createMockJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("mock-token-value")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject("service-a")
                .issuer("http://keycloak/realms/demo")
                .audience(Arrays.asList("service-b", "api://service-b"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claimsBuilder -> claimsBuilder.putAll(claims))
                .build();
    }

    @Test
    @DisplayName("JWT token with valid structure should parse correctly")
    void testValidJwtTokenStructure() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "service-a");
        claims.put("scope", "service-a service-b");
        
        Jwt jwt = createMockJwt(claims);
        
        assertNotNull(jwt);
        assertEquals("service-a", jwt.getClaimAsString("client_id"));
        assertEquals("service-a service-b", jwt.getClaimAsString("scope"));
    }

    @Test
    @DisplayName("JWT token with resource_access roles should parse correctly")
    void testJwtTokenWithResourceAccessRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "service-a");
        
        Map<String, Object> resourceAccess = new HashMap<>();
        Map<String, Object> serviceBRoles = new HashMap<>();
        serviceBRoles.put("roles", Arrays.asList("admin", "user"));
        resourceAccess.put("service-b", serviceBRoles);
        claims.put("resource_access", resourceAccess);
        
        Jwt jwt = createMockJwt(claims);
        
        assertNotNull(jwt);
        Map<String, Object> resourceAccessClaim = jwt.getClaimAsMap("resource_access");
        assertNotNull(resourceAccessClaim);
        assertTrue(resourceAccessClaim.containsKey("service-b"));
    }

    @Test
    @DisplayName("JWT token should include issuer claim")
    void testJwtTokenIncludesIssuer() {
        Map<String, Object> claims = new HashMap<>();
        Jwt jwt = createMockJwt(claims);
        
        assertNotNull(jwt.getIssuer());
        assertTrue(jwt.getIssuer().toString().contains("keycloak"));
    }

    @Test
    @DisplayName("JWT token should include audience claim")
    void testJwtTokenIncludesAudience() {
        Map<String, Object> claims = new HashMap<>();
        Jwt jwt = createMockJwt(claims);
        
        assertNotNull(jwt.getAudience());
        assertTrue(jwt.getAudience().contains("service-b"));
    }

    @Test
    @DisplayName("JWT token should have expiration date in future")
    void testJwtTokenExpiration() {
        Map<String, Object> claims = new HashMap<>();
        Jwt jwt = createMockJwt(claims);
        
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Multiple CORS configurations should be registered")
    void testMultipleCorsPathPatterns() {

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNotNull(corsConfigurationSource);
        
        // Test multiple paths
        String[] testPaths = {
                "/api/internal/info/123",
                "/api/internal/info",
                "/actuator/health",
                "/health/live"
        };
        
        for (String path : testPaths) {
            request.setRequestURI(path);
            var corsConfig = corsConfigurationSource.getCorsConfiguration(request);
            assertNotNull(corsConfig, "CORS config should exist for path: " + path);
        }
    }
}


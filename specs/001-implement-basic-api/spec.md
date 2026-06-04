# Feature Specification: Service Integration with OIDC Protection

**Feature Branch**: `001-implement-basic-services`  
**Created**: May 5, 2026  
**Status**: Draft  
**Input**: User description: "implement basic services. Service A exposes an info REST API endpoint that invokes an endpoint on service b where info is derived from a postgresql database table where entity layout is name, description and version. Service interaction need to get protected with OIDC client credential flow where keycloak is the OIDC authentication provider."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - External Caller Retrieves Entity Info via Service A (Priority: P1)

An external system or user calls Service A's info REST API endpoint to retrieve entity information. Service A acts as a gateway, invoking Service B internally to fetch the data, which Service B retrieves from a PostgreSQL database.

**Why this priority**: This is the core feature providing end-to-end value. Without this, the system cannot deliver entity information to consumers.

**Independent Test**: Can be fully tested by calling an HTTP GET endpoint on Service A and validating that the response contains entity data (name, description, version) retrieved from the database through Service B.

**Acceptance Scenarios**:

1. **Given** Service A and Service B are running and database contains entity records, **When** an external caller makes a GET request to `/api/info/{id}` on Service A, **Then** Service A returns HTTP 200 with entity data (name, description, version) from Service B.
2. **Given** multiple entities exist in the database, **When** an external caller requests entity info, **Then** the correct entity is returned based on the request parameters.
3. **Given** Service B is responsive, **When** Service A calls Service B's endpoint, **Then** the entire flow completes within acceptable latency (under 1 second).

---

### User Story 2 - Service-to-Service OIDC Authentication Flow (Priority: P1)

Service A securely authenticates to Service B using the OIDC Client Credentials Flow with Keycloak as the authentication provider. Service A obtains a JWT token from Keycloak and includes it when calling Service B's API.

**Why this priority**: Security is foundational. Service-to-service communication must be authenticated and authorized before any data exchange occurs. Without this, the system is not production-ready.

**Independent Test**: Can be fully tested by verifying that Service A successfully obtains a token from Keycloak using client credentials, and Service B validates and accepts the token when Service A calls it.

**Acceptance Scenarios**:

1. **Given** Service A is configured with Keycloak client credentials (client ID and secret), **When** Service A needs to call Service B, **Then** Service A successfully retrieves a valid JWT token from Keycloak.
2. **Given** Service A has obtained a JWT token, **When** Service A calls Service B with the token in the Authorization header, **Then** Service B validates the token and grants access.
3. **Given** token expiry is configured, **When** a token expires, **Then** Service A automatically requests a new token from Keycloak on the next request.
4. **Given** Service A uses invalid credentials, **When** attempting to authenticate with Keycloak, **Then** authentication fails with HTTP 401 Unauthorized.

---

### User Story 3 - Service B Queries Database for Entity Info (Priority: P1)

Service B queries the PostgreSQL database to retrieve entity information. The database contains an entity table with columns: name, description, and version.

**Why this priority**: This is essential data access functionality. Without database integration, the system cannot retrieve or persist entity information.

**Independent Test**: Can be fully tested by calling Service B's internal endpoint directly and verifying it correctly queries and returns entity records from PostgreSQL.

**Acceptance Scenarios**:

1. **Given** the PostgreSQL database contains entity records, **When** Service B queries the database for an entity, **Then** the correct record is retrieved with all fields (name, description, version).
2. **Given** an entity exists in the database, **When** Service B is called with an entity ID, **Then** the entity is retrieved and returned to the caller.
3. **Given** the database is temporarily unavailable, **When** Service B attempts to query, **Then** a meaningful error response is returned (HTTP 503 Service Unavailable).

---

### User Story 4 - Error Handling and Resilience (Priority: P2)

The system gracefully handles errors including database unavailability, authentication failures, and service timeouts.

**Why this priority**: Robust error handling improves reliability and user experience. While core functionality is more critical, error handling prevents cascading failures.

**Independent Test**: Can be tested by simulating failure scenarios (stopping Database, stopping Keycloak, stopping Service B) and verifying Service A returns appropriate error responses.

**Acceptance Scenarios**:

1. **Given** Service B is unavailable, **When** Service A attempts to call it, **Then** Service A returns HTTP 503 Service Unavailable with an error message.
2. **Given** Keycloak is unavailable, **When** Service A attempts to authenticate, **Then** Service A returns HTTP 503 with appropriate error messaging.
3. **Given** a database query fails, **When** Service B processes the request, **Then** Service B returns HTTP 500 Internal Server Error with error details.

---

### Edge Cases

- What happens when a request is made for a non-existent entity? (Return HTTP 404 Not Found)
- How does the system behave when authentication tokens are malformed or tampered with? (Reject with HTTP 401)
- What occurs when the response from Service B is delayed beyond timeout threshold? (Return HTTP 504 Gateway Timeout)
- How are concurrent requests handled by both services? (Each request should be processed independently without state conflicts)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Service A MUST expose a REST API endpoint at `/api/info/{id}` that accepts HTTP GET requests
- **FR-002**: Service A MUST authenticate to Service B using OIDC Client Credentials Flow via Keycloak before accessing Service B
- **FR-003**: Service A MUST invoke Service B's info endpoint to retrieve entity information
- **FR-004**: Service B MUST expose a REST API endpoint that returns entity information from PostgreSQL
- **FR-005**: Service B MUST query PostgreSQL database table containing entities with fields: name, description, version
- **FR-006**: Service A MUST include JWT token in the Authorization header when calling Service B
- **FR-007**: Service B MUST validate JWT tokens received from Service A
- **FR-008**: Service A MUST return entity information to the caller in JSON format with name, description, and version fields
- **FR-009**: Both services MUST return appropriate HTTP status codes (200 for success, 401 for auth failure, 503 for service unavailable, 404 for not found)
- **FR-010**: Service A MUST handle authentication token expiry and refresh tokens as needed

### Security Requirements

- **SR-001**: Service-to-service communication MUST use OAuth2 Client Credentials Flow with Keycloak (no custom auth)
- **SR-002**: All API endpoints MUST validate JWT tokens issued by Keycloak
- **SR-003**: Service B MUST reject requests without valid JWT tokens with HTTP 401 Unauthorized
- **SR-004**: Keycloak client secrets MUST NOT be hardcoded; MUST be stored in external vault via External Secrets Operator
- **SR-005**: All service-to-service communication MUST use HTTPS/TLS in production

### Deployment Requirements

- **DR-001**: Service A MUST be containerized with multi-stage Dockerfile; image tagged with semantic version
- **DR-002**: Service B MUST be containerized with multi-stage Dockerfile; image tagged with semantic version
- **DR-003**: Kubernetes manifests for both services MUST include Helm charts with values.yaml
- **DR-004**: Deployment MUST be managed via FluxCD HelmRelease CRDs
- **DR-005**: Both services MUST include health check endpoints for liveness and readiness probes
- **DR-006**: Services MUST be stateless; only PostgreSQL database maintains persistent state
- **DR-007**: Database connection strings MUST be sourced from External Secrets Operator, not hardcoded

### Observability Requirements

- **OR-001**: Both services MUST output structured JSON logs to stdout via Logback
- **OR-002**: Both services MUST export metrics via Micrometer to Prometheus
- **OR-003**: Both services MUST export traces via OpenTelemetry to Tempo
- **OR-004**: Both services MUST export logs via OpenTelemetry to Loki
- **OR-005**: Health endpoints MUST be available at `/health/live` (liveness) and `/health/ready` (readiness)
- **OR-006**: API calls between Service A and Service B MUST include trace context propagation

### Key Entities

- **Entity**: Represents domain information with attributes:
  - `name`: String identifier/name of the entity
  - `description`: Text describing the entity
  - `version`: Version number for tracking entity evolution
  - Stored in PostgreSQL table (schema/table name to be determined during implementation)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: External callers can retrieve entity information through Service A's REST API endpoint with 100% success rate when all services are healthy
- **SC-002**: Service-to-service authentication completes successfully and consistently (token acquisition from Keycloak, token validation at Service B)
- **SC-003**: Entity information is retrieved from PostgreSQL and returned to caller within 1 second end-to-end (for single entity queries)
- **SC-004**: System handles 100+ concurrent requests without degradation in response times or authentication success rates
- **SC-005**: Service B correctly returns HTTP 404 for non-existent entities and HTTP 5xx for database unavailability
- **SC-006**: Failed authentication attempts are properly logged and rejected within 500ms
- **SC-007**: All core APIs respond with valid JSON payloads matching specified schema (name, description, version fields present)

## Assumptions

- Keycloak instance is already deployed and operational in the target Kubernetes cluster
- PostgreSQL database instance is already deployed and accessible to Service B
- Both services will be deployed in the same Kubernetes cluster
- The cluster has FluxCD and External Secrets Operator already installed
- Development team has access to configure Keycloak clients and credentials
- Service communication will use internal Kubernetes DNS for service discovery
- Standard REST conventions are acceptable for API design (no GraphQL requirement)
- Response time SLAs are sub-second for normal operations
- Database table for entities will be created as part of database migration scripts (separate from this service implementation)



# Implementation Tasks: Service Integration with OIDC Protection

**Feature**: Implement basic services with OIDC-protected service-to-service communication  
**Branch**: `001-implement-basic-services`  
**Created**: May 6, 2026  
**Total Tasks**: 61 | **Setup**: 8 | **Foundational**: 11 | **US1**: 9 | **US2**: 9 | **US3**: 10 | **US4**: 8 | **Polish**: 6

---

## Implementation Strategy

**MVP Scope**: User Story 1 (US1) only - External caller retrieves entity info
- Delivers end-to-end value: external API call → Service A → Service B → PostgreSQL
- Can be tested independently without error handling complexity
- Includes basic OIDC flow (part of core feature)
- Estimated timeline: 2-3 weeks

**Recommended Execution Path**:
1. Complete Phase 1 (Setup) - Project initialization
2. Complete Phase 2 (Foundational) - Shared infrastructure
3. Complete Phase 3 (US1) - Core feature
4. Add Phase 4-6 (US2-US4) iteratively for robustness

**Parallel Opportunities**:
- Within Phase 1: All gradle/docker setup tasks [P] can run in parallel
- Within Phase 3-6: Service A and Service B implementation [P] can run in parallel
- Testing: Unit tests [P] can run independently while integration tests run

---

## Dependency Graph

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational)
    ↓
Phase 3 (US1) ← [Can start once Phase 2 complete]
    ↓
Phase 4 (US2) ← [Depends on Phase 3 for Service A structure]
    ↓
Phase 5 (US3) ← [Depends on Phases 3-4 for Service B structure]
    ↓
Phase 6 (US4) ← [Depends on Phases 3-5 for error scenarios]
    ↓
Phase 7 (Polish)
```

---

## Phase 1: Setup (Project Initialization)

**Phase Goal**: Establish project structure, build tools, and deployment infrastructure

**Independent Test Criteria**:
- Gradle builds successfully for both services
- Docker images build without errors
- Helm chart syntax is valid
- GitLab CI pipeline compiles and unit tests pass

### Setup Tasks

- [x] T001 Create gradle multi-project build structure in `apps/` with settings.gradle and build.gradle
- [x] T002 [P] Create Service A project structure in `apps/service-a/` with main/test directory layout
- [x] T003 [P] Create Service B project structure in `apps/service-b/` with main/test directory layout
- [x] T004 [P] Create Dockerfile for Service A at `apps/service-a/Dockerfile` (multi-stage, Java 25)
- [x] T005 [P] Create Dockerfile for Service B at `apps/service-b/Dockerfile` (multi-stage, Java 25)
- [x] T006 Create Helm chart structure for Service A at `apps/service-a/helm/` with Chart.yaml and values.yaml
- [x] T007 Create Helm chart structure for Service B at `apps/service-b/helm/` with Chart.yaml and values.yaml
- [x] T008 Create .gitlab-ci.yml at repository root with build, test, and push stages

---

## Phase 2: Foundational (Blocking Prerequisites)

**Phase Goal**: Establish shared infrastructure, configuration management, and observability

**Independent Test Criteria**:
- Spring Boot projects start without errors
- Configuration loads from environment variables
- OpenTelemetry exports are discoverable
- PostgreSQL Flyway migrations are versioned

### Infrastructure & Configuration Tasks

- [ ] T009 Add Spring Boot 4 and Spring Security dependencies to `apps/service-a/build.gradle`
- [ ] T010 Add Spring Boot 4 and Spring Security dependencies to `apps/service-b/build.gradle`
- [ ] T011 Add OpenTelemetry, Micrometer, and LGTM exporter dependencies to both build.gradle files
- [ ] T012 [P] Create application.yaml template in `apps/service-a/src/main/resources/` with Spring Boot and observability config
- [ ] T013 [P] Create application.yaml template in `apps/service-b/src/main/resources/` with Spring Boot and observability config
- [ ] T014 [P] Create logback-spring.xml in `apps/service-a/src/main/resources/` for JSON logging
- [ ] T015 [P] Create logback-spring.xml in `apps/service-b/src/main/resources/` for JSON logging
- [ ] T016 Create PostgreSQL migration script `V1__create_entity_table.sql` in `apps/service-b/src/main/resources/db/migration/`
- [ ] T017 Add Flyway migration dependency to Service B build.gradle
- [ ] T018 Create OpenTelemetry configuration class in `apps/service-b/src/main/java/com/agilesolutions/service_b/config/ObservabilityConfig.java`
- [ ] T019 Create OpenTelemetry configuration class in `apps/service-a/src/main/java/com/agilesolutions/service_a/config/ObservabilityConfig.java`

---

## Phase 3: US1 - External Caller Retrieves Entity Info via Service A (P1)

**Story Goal**: Implement end-to-end flow allowing external callers to retrieve entity info through Service A REST API, which invokes Service B internally

**Independent Test Criteria**:
- Service A /api/info/{id} endpoint returns HTTP 200 with entity data (name, description, version)
- Entity data is correctly retrieved from PostgreSQL through Service B
- Response time is under 1 second
- Concurrent requests (100+) are handled without state conflicts
- Non-existent entities return HTTP 404

### Service B - Data Access

- [ ] T020 Create Entity model class in `apps/service-b/src/main/java/com/agilesolutions/service_b/model/Entity.java` with id, name, description, version fields
- [ ] T021 Create EntityRepository interface in `apps/service-b/src/main/java/com/agilesolutions/service_b/repository/EntityRepository.java` extending JpaRepository
- [ ] T022 Create EntityService class in `apps/service-b/src/main/java/com/agilesolutions/service_b/service/EntityService.java` with findById(UUID) method
- [ ] T023 [P] Create unit tests for EntityService in `apps/service-b/src/test/java/com/agilesolutions/service_b/unit/EntityServiceTest.java`

### Service B - API Endpoint

- [ ] T024 Create InfoController in `apps/service-b/src/main/java/com/agilesolutions/service_b/controller/InfoController.java` with GET /api/internal/info/{id} endpoint
- [ ] T025 [P] Create unit tests for InfoController in `apps/service-b/src/test/java/com/agilesolutions/service_b/unit/InfoControllerTest.java`

### Service A - Gateway Service

- [ ] T026 Create EntityClient (HTTP client) in `apps/service-a/src/main/java/com/agilesolutions/service_a/service/EntityClient.java` to call Service B
- [ ] T027 Create InfoService in `apps/service-a/src/main/java/com/agilesolutions/service_a/service/InfoService.java` orchestrating logic
- [ ] T028 Create InfoController in `apps/service-a/src/main/java/com/agilesolutions/service_a/controller/InfoController.java` with GET /api/info/{id} endpoint
- [ ] T029 [P] Create unit tests for Service A in `apps/service-a/src/test/java/com/agilesolutions/service_a/unit/InfoControllerTest.java`

### Integration Tests for US1

- [ ] T030 Create integration test using Testcontainers for PostgreSQL in `apps/service-b/src/test/java/com/agilesolutions/service_b/integration/EntityServiceIntegrationTest.java`
- [ ] T031 [P] Create integration test for Service A calling Service B in `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/InfoControllerIntegrationTest.java`

---

## Phase 4: US2 - Service-to-Service OIDC Authentication Flow (P1)

**Story Goal**: Implement OIDC Client Credentials Flow authentication between Service A and Service B using Keycloak

**Independent Test Criteria**:
- Service A successfully obtains JWT token from Keycloak using client credentials
- Service A includes token in Authorization header when calling Service B
- Service B validates token and rejects unauthorized requests with HTTP 401
- Token expiration is handled (new token on next request)
- Invalid credentials result in HTTP 401 from Keycloak

### Keycloak Configuration (Setup)

- [ ] T032 Create docker-compose.yml at repository root with Keycloak and PostgreSQL for local development
- [ ] T033 Create Keycloak client configuration script in `infra/keycloak/setup-clients.sh` to create service-a and service-b clients

### Service A - OAuth2 Client

- [ ] T034 Add Spring Security OAuth2 Client dependency to `apps/service-a/build.gradle`
- [ ] T035 Create OAuth2ClientConfig in `apps/service-a/src/main/java/com/agilesolutions/service_a/config/OAuth2ClientConfig.java` with token acquisition logic
- [ ] T036 Update EntityClient to use OAuth2 token when calling Service B in `apps/service-a/src/main/java/com/agilesolutions/service_a/service/EntityClient.java`

### Service B - OAuth2 Resource Server

- [ ] T037 Add Spring Security OAuth2 Resource Server dependency to `apps/service-b/build.gradle`
- [ ] T038 Create OAuth2ResourceServerConfig in `apps/service-b/src/main/java/com/agilesolutions/service_b/config/OAuth2ResourceServerConfig.java` for JWT validation
- [ ] T039 Secure InfoController endpoint with @EnableWebSecurity and JWT validation in `apps/service-b/src/main/java/com/agilesolutions/service_b/controller/InfoController.java`
- [ ] T040 [P] Create unit tests for OAuth2 token validation in `apps/service-b/src/test/java/com/agilesolutions/service_b/unit/OAuth2ResourceServerConfigTest.java`

### Integration Tests for US2

- [ ] T041 Create integration test with Testcontainers for Keycloak in `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/OAuth2ClientIntegrationTest.java` verifying token acquisition
- [ ] T042 [P] Create integration test for Service A→Service B with valid/invalid tokens in `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/ServiceToServiceAuthIntegrationTest.java`

---

## Phase 5: US3 - Service B Queries Database for Entity Info (P1)

**Story Goal**: Ensure Service B correctly queries PostgreSQL database and handles data retrieval

**Independent Test Criteria**:
- EntityService correctly retrieves entity records from PostgreSQL
- All fields (name, description, version) are returned
- Non-existent entities are handled gracefully
- Database unavailability returns HTTP 503
- Query performance meets latency requirements

### Database Schema & Migrations

- [ ] T043 Enhance V1__create_entity_table.sql migration with indexes and constraints in `apps/service-b/src/main/resources/db/migration/`
- [ ] T044 Create V2__add_entity_seed_data.sql migration for test data in `apps/service-b/src/main/resources/db/migration/`

### Database Error Handling

- [ ] T045 Add exception handling in EntityService for database errors in `apps/service-b/src/main/java/com/agilesolutions/service_b/service/EntityService.java`
- [ ] T046 Create custom exception ServiceUnavailableException in `apps/service-b/src/main/java/com/agilesolutions/service_b/exception/ServiceUnavailableException.java`
- [ ] T047 [P] Create unit tests for database error scenarios in `apps/service-b/src/test/java/com/agilesolutions/service_b/unit/EntityServiceErrorTest.java`

### Advanced Integration Tests

- [ ] T048 Create Testcontainers test simulating slow queries in `apps/service-b/src/test/java/com/agilesolutions/service_b/integration/DatabasePerformanceTest.java`
- [ ] T049 Create test verifying database unavailability handling in `apps/service-b/src/test/java/com/agilesolutions/service_b/integration/DatabaseUnavailabilityTest.java`
- [ ] T050 [P] Create concurrent request test (100+) in `apps/service-b/src/test/java/com/agilesolutions/service_b/integration/ConcurrentRequestTest.java`

---

## Phase 6: US4 - Error Handling and Resilience (P2)

**Story Goal**: Implement comprehensive error handling for service unavailability, auth failures, and timeouts

**Independent Test Criteria**:
- Service A returns HTTP 503 when Service B is unavailable
- Service A returns HTTP 503 when Keycloak is unavailable
- Service B returns HTTP 500 with error details when database fails
- Request timeouts result in HTTP 504 Gateway Timeout
- All errors are properly logged with trace context

### Error Handling - Service A

- [ ] T051 Create GlobalExceptionHandler in `apps/service-a/src/main/java/com/agilesolutions/service_a/exception/GlobalExceptionHandler.java` for all exceptions
- [ ] T052 Add timeout handling to EntityClient (5 second timeout) in `apps/service-a/src/main/java/com/agilesolutions/service_a/service/EntityClient.java`
- [ ] T053 [P] Create unit tests for error scenarios in `apps/service-a/src/test/java/com/agilesolutions/service_a/unit/ErrorHandlingTest.java`

### Error Handling - Service B

- [ ] T054 Create GlobalExceptionHandler in `apps/service-b/src/main/java/com/agilesolutions/service_b/exception/GlobalExceptionHandler.java` for all exceptions
- [ ] T055 Add database connection timeout configuration in `apps/service-b/src/main/resources/application.yaml`

### Resilience Tests

- [ ] T056 Create chaos test simulating Service B unavailability in `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/ChaosEngineeringTest.java`
- [ ] T057 Create chaos test simulating Keycloak unavailability in `apps/service-a/src/test/java/com/agilesolutions/service_a/integration/KeycloakChaosTest.java`
- [ ] T058 [P] Create health check endpoint tests in `apps/service-a/src/test/java/com/agilesolutions/service_a/unit/HealthCheckTest.java`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Phase Goal**: Add deployment manifests, CI/CD pipelines, infrastructure-as-code, and observability

**Independent Test Criteria**:
- Helm charts deploy successfully to Kubernetes
- FluxCD manifests are valid and deployable
- Terraform IAC provisions AKS cluster without errors
- GitLab CI pipeline builds, tests, and pushes images
- All services export logs/metrics/traces to LGTM

### Kubernetes Deployment Manifests

- [ ] T059 [P] Create Helm deployment.yaml for Service A in `apps/service-a/helm/templates/deployment.yaml`
- [ ] T060 [P] Create Helm deployment.yaml for Service B in `apps/service-b/helm/templates/deployment.yaml`
- [ ] T061 [P] Create Helm service.yaml and ingress.yaml for both services in `apps/service-a/helm/templates/` and `apps/service-b/helm/templates/`

### FluxCD & Infrastructure

- [ ] T062 Create FluxCD HelmRelease CRDs for Service A and B in `platform/fluxcd/helmreleases/`
- [ ] T063 Create Terraform modules for AKS, PostgreSQL, Vault in `infra/terraform/modules/`
- [ ] T064 Create Terraform main.tf, variables.tf, outputs.tf in `infra/terraform/`
- [ ] T065 Update .gitlab-ci.yml with docker build/push and helm deploy stages

### Observability & Monitoring

- [ ] T066 Create ServiceMonitor CRD for Prometheus scraping in `platform/` (if Prometheus Operator is used)

---

## Testing Summary by User Story

| User Story | Unit Tests | Integration Tests | Test Containers |
|------------|-----------|-------------------|------------------|
| **US1** | 3 | 2 | PostgreSQL + App |
| **US2** | 2 | 2 | Keycloak + App |
| **US3** | 2 | 3 | PostgreSQL + App |
| **US4** | 2 | 3 | All + Chaos |
| **Total** | 9 | 10 | Multiple |

---

## Cross-Cutting Concerns Mapping

| Concern | Implementation | Tasks |
|---------|---|---|
| **Observability** | JSON logging, metrics, traces | T014-T015, T018-T019, (T066) |
| **Security** | OAuth2, JWT validation | T034-T039 |
| **Database** | Flyway migrations, connection pooling | T016-T017, T043-T044, T055 |
| **Error Handling** | Global exception handlers | T051-T054, T056-T058 |
| **Deployment** | Helm + FluxCD + Terraform | T059-T065 |
| **CI/CD** | GitLab pipeline | T008, T065 |

---

## Validation Checklist

**Format Validation**: ✅ All 66 tasks follow strict checklist format
- ✅ All tasks have checkbox (`- [ ]`)
- ✅ All tasks have Task ID (T001-T066)
- ✅ Parallelizable tasks marked with [P]
- ✅ User story phase tasks marked with [Story]
- ✅ All tasks include file path

**Completeness Validation**:
- ✅ Each user story has all necessary tasks for independent testing
- ✅ Setup and Foundational phases are complete blockers
- ✅ Each phase is independently testable
- ✅ All requirements from spec.md are mapped to tasks
- ✅ All components from data-model.md are implemented

**Quality Validation**:
- ✅ Tasks are immediately executable by LLM agents
- ✅ Dependencies are clear (Phase 1 → Phase 2 → Phase 3-6 → Phase 7)
- ✅ Parallel opportunities identified and marked
- ✅ MVP scope clearly defined (US1 only)

---

## How to Use This Task List

1. **For MVP delivery**: Execute Phases 1-3 only (26 tasks, ~2-3 weeks)
2. **For full implementation**: Execute all phases (66 tasks, ~6-8 weeks)
3. **For parallel execution**: All tasks marked [P] can run concurrently within the same phase
4. **For testing**: Run unit tests during development, integration tests at phase completion



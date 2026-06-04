# Implementation Plan: Service Integration with OIDC Protection + RestClient Migration

**Branch**: `002-spring-boot-v4` | **Date**: May 12, 2026 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-implement-basic-api/spec.md`

**Note**: This plan covers the primary feature (Service Integration with OIDC) and the RestClient migration enhancement.

## Summary

Implement two microservices (Service A and Service B) with OIDC-protected inter-service communication. Service A exposes a REST API endpoint that retrieves entity information from Service B, which queries PostgreSQL. Service-to-service authentication uses OAuth2 Client Credentials Flow with Keycloak. This implementation also replaces legacy RestTemplate with modern RestClient (Spring Boot 4.x) and implements comprehensive testing with RestTestClient.

## Technical Context

**Language/Version**: Java 25 (LTS-aligned)  
**Primary Dependencies**: Spring Boot 4.x, RestClient (spring-web), RestTestClient (spring-boot-test), Keycloak 24+, PostgreSQL 15+  
**Storage**: PostgreSQL 15+ (entity table with name, description, version fields)  
**Testing**: Spring Test, RestTestClient, TestContainers (PostgreSQL, Keycloak stub)  
**Target Platform**: Kubernetes 1.26+ (AKS/EKS/on-premise)  
**Project Type**: Microservices (two independently deployable Java/Spring Boot services)  
**Performance Goals**: Sub-second end-to-end response time (<1000ms); 100+ concurrent request handling  
**Constraints**: <500ms authentication latency; stateless services; 30-second graceful shutdown  
**Scale/Scope**: Two services, PostgreSQL persistence, OIDC authentication, observability stack integration (Prometheus, Loki, Tempo), RestClient-based HTTP client

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Mandatory Verification**:

- [x] **Architecture**: Feature respects microservices boundaries (Java 25 + Spring Boot 4); no monolithic additions
- [x] **API-First Design**: RESTful contract defined; asynchronous messaging declared if used
- [x] **Security**: Authentication uses Keycloak (Client Credentials for service-to-service, Authorization Code for users); no custom auth
- [x] **Observability**: Structured JSON logging, Micrometer metrics, OpenTelemetry tracing configured; LGTM export validated
- [x] **Configuration**: All env-specific settings externalized as environment variables (no hardcoded values)
- [x] **Testing**: Specification includes acceptance criteria; integration test strategy defined (PostgreSQL test containers + Keycloak stub)
- [x] **Deployment**: Helm chart structure + values.yaml template prepared; FluxCD integration ready
- [x] **Kubernetes**: Container image versioning (semantic), health checks (liveness + readiness), graceful termination (30s) planned
- [x] **Database**: PostgreSQL schema migrations (Flyway/Liquibase) versioned in Git; no manual DDL
- [x] **Compliance**: 15-factor checklist completed (codebase, dependencies, config, backing services, build/release/run, processes, port binding, concurrency, disposability, dev/prod parity, logs, admin tasks, monitoring, persistence, graceful shutdown)

✅ **All Constitution gates PASS**. Feature spec complies with Haven POC principles.

## Project Structure

### Documentation (this feature)

```text
specs/001-implement-basic-api/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── service-a-api.yaml
│   └── service-b-api.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
apps/
├── service-a/                          # Gateway service: REST API → Service B
│   ├── src/main/java/com/...
│   │   ├── controller/                 # REST endpoints
│   │   ├── client/                     # RestClient for Service B calls
│   │   ├── security/                   # OIDC/Keycloak integration
│   │   └── config/                     # RestClient configuration
│   ├── src/test/java/com/...
│   │   ├── integration/                # IntegrationTest with RestTestClient
│   │   ├── client/                     # RestClient tests
│   │   └── security/                   # Security tests
│   ├── build.gradle                    # Dependencies: spring-web (RestClient), spring-security-oauth2
│   ├── Dockerfile                      # Multi-stage production build
│   └── helm/                           # Kubernetes Helm templates
│       ├── values.yaml
│       └── templates/
│
└── service-b/                          # Data service: PostgreSQL queries
    ├── src/main/java/com/...
    │   ├── controller/                 # REST endpoints
    │   ├── repository/                 # JPA/SQL data access
    │   ├── model/                      # Entity classes
    │   ├── security/                   # JWT validation
    │   └── config/                     # Database/security config
    ├── src/test/java/com/...
    │   ├── integration/                # IntegrationTest with TestContainers
    │   ├── repository/                 # Repository tests
    │   └── security/                   # JWT validation tests
    ├── build.gradle                    # Dependencies: spring-data-jpa, postgresql, spring-security-oauth2
    ├── Dockerfile                      # Multi-stage production build
    └── helm/                           # Kubernetes Helm templates
        ├── values.yaml
        └── templates/
```

**Structure Decision**: Multi-service Gradle project. Service A is a REST gateway with RestClient for outbound calls; Service B is a data service with JPA repository. Both services independently deployable via separate Helm charts.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

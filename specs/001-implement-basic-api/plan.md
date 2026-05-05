# Implementation Plan: Service Integration with OIDC Protection

**Branch**: `001-implement-basic-services` | **Date**: May 5, 2026 | **Spec**: specs/001-implement-basic-api/spec.md
**Input**: Feature specification from `/specs/001-implement-basic-api/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Implement two Spring Boot microservices (Service A and Service B) with OIDC-protected service-to-service communication using Keycloak. Service A exposes a REST API endpoint that retrieves entity information from Service B, which queries a PostgreSQL database. Include unit tests, Testcontainers integration tests, Helm charts, GitLab CI/CD pipelines, FluxCD manifests, and Terraform for AKS infrastructure.

## Technical Context

**Language/Version**: Java 25 with Spring Boot 4  
**Primary Dependencies**: Spring Boot 4, Spring Security, Spring Data JPA, Spring Web, OpenTelemetry, Micrometer, Testcontainers  
**Storage**: PostgreSQL 15+  
**Testing**: JUnit 5, Testcontainers (PostgreSQL, Keycloak)  
**Target Platform**: Kubernetes (AKS)  
**Project Type**: web-service (microservices)  
**Performance Goals**: 1 second end-to-end latency for entity retrieval  
**Constraints**: <1 second p95 latency, 100% success rate for healthy services, 100+ concurrent requests  
**Scale/Scope**: 100+ concurrent requests, two services, PostgreSQL database  

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

## Project Structure

### Documentation (this feature)

```text
specs/001-implement-basic-api/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
apps/
├── service-a/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── helm/
│   │   ├── Chart.yaml
│   │   ├── values.yaml
│   │   └── templates/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       ├── ingress.yaml
│   │       └── configmap.yaml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── agilesolutions/
│   │   │   │           └── service_a/
│   │   │   │               ├── config/
│   │   │   │               ├── controller/
│   │   │   │               ├── service/
│   │   │   │               └── model/
│   │   │   └── resources/
│   │   │       ├── application.yaml
│   │   │       └── db/migration/
│   │   └── test/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── agilesolutions/
│   │       │           └── service_a/
│   │       │               ├── unit/
│   │       │               └── integration/
│   │       └── resources/
│   └── settings.gradle
├── service-b/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── helm/
│   │   ├── Chart.yaml
│   │   ├── values.yaml
│   │   └── templates/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       ├── ingress.yaml
│   │       └── configmap.yaml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── agilesolutions/
│   │   │   │           └── service_b/
│   │   │   │               ├── config/
│   │   │   │               ├── controller/
│   │   │   │               ├── service/
│   │   │   │               ├── repository/
│   │   │   │               └── model/
│   │   │   └── resources/
│   │   │       ├── application.yaml
│   │   │       ├── logback-spring.xml
│   │   │       └── db/migration/
│   │   └── test/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── agilesolutions/
│   │       │           └── service_b/
│   │       │               ├── unit/
│   │       │               └── integration/
│   │       └── resources/
│   └── settings.gradle
infra/
├── terraform/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── modules/
│       ├── aks/
│       ├── postgresql/
│       ├── vault/
│       └── rbac/
platform/
├── fluxcd/
│   ├── helmreleases/
│   │   ├── service-a-helmrelease.yaml
│   │   ├── service-b-helmrelease.yaml
│   │   ├── keycloak-helmrelease.yaml
│   │   ├── nginx-helmrelease.yaml
│   │   └── postgresql-helmrelease.yaml
│   └── kustomization.yaml
.gitlab-ci.yml
```

**Structure Decision**: Multi-service microservices architecture with separate apps for Service A and Service B, infrastructure as code with Terraform, GitOps with FluxCD, and CI/CD with GitLab.

## Complexity Tracking

No violations.

<!-- 
  SYNC IMPACT REPORT
  
  Version: 1.0.0 (Initial)
  Ratified: 2026-05-05
  
  Principles Defined:
  - I. Microservices Architecture (API-Driven)
  - II. Security-First Authentication
  - III. 15-Factor Compliance & Observability
  - IV. GitOps Delivery Model
  - V. Infrastructure as Code
  - VI. Kubernetes Containerization
  
  Templates to Update:
  - ✅ plan-template.md (Constitution Check added)
  - ✅ spec-template.md (Security & deployment constraints noted)
  - ✅ tasks-template.md (Task categories aligned with principles)
  
  No previously amended versions (initial constitution).
-->

# Haven POC Constitution

The Haven POC reference project establishes a standard for platform-independent cloud hosting and demonstrates microservices best practices on Kubernetes. This constitution defines non-negotiable principles for development, deployment, and governance.

## Core Principles

### I. Microservices Architecture (API-Driven)

Every feature is delivered as an independently deployable service. Applications MUST:
- Use Java 25 with Spring Boot 4 as the base framework
- Expose RESTful APIs as the primary integration contract
- Implement loose coupling through asynchronous messaging where appropriate (e.g., events)
- Maintain service-level isolation: each service owns its database (no shared data layer)
- Be independently scalable and deployable via Kubernetes
- Follow clear separation of concerns: no cross-service dependencies that block deployment

**Rationale**: Independent deployability reduces blast radius of changes, enables team parallelization, and aligns with 15-factor principles for scalability and resilience.

### II. Security-First Authentication & Authorization

Authentication and authorization MUST use:

- **Service-to-Service Communication**: OAuth2 Client Credentials Flow
  - All inter-service requests MUST present a Bearer token issued by Keycloak
  - Service identity verified via JWT validation at request boundaries
  - Token scope limited to necessary API permissions (principle of least privilege)
  
- **End-User Authentication**: OAuth2 Authorization Code Flow with OIDC
  - User login and session management delegated to Keycloak (no custom auth)
  - Tokens validated and refreshed via standard OIDC mechanisms
  - No plaintext password storage or transmission within services

- **Identity Provider**: Keycloak (mandatory)
  - Single source of truth for all identity management
  - Support realm-based multi-tenancy when required
  - Audit all login and token events

**Rationale**: OIDC + Keycloak provides defense-in-depth, auditability, and compliance with Haven security standards. Client Credentials prevents service impersonation; Authorization Code prevents credential exposure to untrusted clients.

### III. 15-Factor Compliance & Observability

Applications MUST adhere to the 15-factor methodology:

1. **Codebase**: Single codebase per service, version controlled in Git
2. **Dependencies**: Explicitly declared in Gradle (Groovy DSL), no implicit system libraries
3. **Config**: Environment variables for all environment-specific settings (no hardcoded values)
4. **Backing services**: All external services (databases, message brokers) treated as attached resources
5. **Build/Release/Run**: Strict separation enforced via Gradle build, Docker images, Kubernetes runtime
6. **Processes**: Stateless execution; session/state persisted in PostgreSQL
7. **Port binding**: Services self-contained; port binding via Spring Boot embedded server
8. **Concurrency**: Processes managed by Kubernetes (no in-process thread management for scaling)
9. **Disposability**: Fast startup/shutdown; Kubernetes health checks (liveness, readiness)
10. **Dev/Prod Parity**: Identical runtime in all environments (Docker)
11. **Logs**: Structured JSON logs to stdout via Logback; LGTM stack aggregates
12. **Admin Tasks**: One-off tasks via Kubernetes Jobs (not long-running processes)
13. **Monitoring/Observability**: Metrics + traces + logs via OpenTelemetry → LGTM (Loki, Grafana, Tempo, Mimir)
14. **Database Persistence**: PostgreSQL as default; schema versioned via Flyway/Liquibase migrations
15. **Graceful Shutdown**: 30-second grace period to drain connections; no abrupt termination

**Rationale**: 15-factor ensures operational consistency, reduces surprises in production, and enables safe horizontal scaling.

### IV. GitOps Delivery Model

Deployments MUST use GitOps via FluxCD as the reconciliation engine:

- **Single Source of Truth**: All infrastructure and application state declared in Git
- **Reconciliation Engine**: FluxCD monitors Git repository and applies changes to Kubernetes
- **Packager**: Helm for templating service deployments; HelmRelease custom resources define versioning
- **Kustomization**: Kustomize for composing and patching manifests; only Helm + Kustomize reconcilers allowed
- **No Ad-Hoc Kubectl**: Direct cluster modifications forbidden; all changes via Git commit
- **Declarative Manifests**: Infrastructure defined via Helm values + Kustomize patches, not imperative commands
- **Audit Trail**: Every deployment traceable to specific Git commit and author

**Rationale**: GitOps provides auditability, reproducibility, disaster recovery, and rollback capabilities. Single engine (FluxCD) prevents tool fragmentation.

### V. Infrastructure as Code

All infrastructure MUST be version-controlled and reproducible:

- **Terraform**: Infrastructure provisioned exclusively via Terraform
- **State Management**: Terraform state stored in remote backend (e.g., Azure Storage, S3)
- **Modules**: Reusable Terraform modules for networking, compute, storage, identity
- **No Manual Console Access**: Infrastructure changes only via Terraform + Git
- **Secrets Management**: External Secrets Operator (ESO) for credential injection; no secrets in Git

**Rationale**: IaC enables consistent, auditable infrastructure provisioning; prevents configuration drift; supports disaster recovery.

### VI. Kubernetes Containerization

All services MUST be containerized and orchestrated on Kubernetes:

- **Dockerfile**: Single, production-ready Dockerfile per service (multi-stage builds for size optimization)
- **Image Registry**: Images tagged with semantic version (`vMAJOR.MINOR.PATCH`) and pushed to container registry
- **Resource Requests/Limits**: CPU and memory requests/limits defined in Kubernetes manifests
- **Health Checks**: Liveness probe (server health), readiness probe (traffic acceptance)
- **Graceful Termination**: Services listen to SIGTERM and drain connections within 30 seconds
- **Pod Disruption Budgets**: Defined where appropriate to prevent cascading failures
- **Network Policies**: Service-to-service communication restricted via Kubernetes NetworkPolicy when required

**Rationale**: Kubernetes standardizes deployments, scaling, and failure recovery; containers ensure environment parity.

## Technology Stack & Constraints

### Language & Build
- **Language**: Java 25 (LTS-aligned, latest stable)
- **Framework**: Spring Boot 4.x
- **Build System**: Gradle 8.x with Groovy DSL (no Maven)
- **Dependency Management**: Gradle dependency locking for reproducible builds

### Database & Persistence
- **Primary Database**: PostgreSQL 15+ (all transactional data)
- **Schema Management**: Flyway or Liquibase for versioned migrations
- **Connection Pooling**: HikariCP (default in Spring Boot)
- **No ORM Lock-In**: Use JPA + Hibernate for ORM; raw SQL acceptable for complex queries

### Observability
- **Logging**: Structured JSON via Logback to stdout
- **Metrics**: Micrometer for application metrics → Prometheus (Mimir)
- **Tracing**: OpenTelemetry Java agent → Tempo
- **Logs**: OpenTelemetry SDK → Loki
- **Visualization**: Grafana dashboards for all signals
- **Protocol**: OTLP (OpenTelemetry Protocol) for all telemetry export

### Deployment
- **Runtime**: Kubernetes 1.26+ (AKS, EKS, or on-premise)
- **Package Manager**: Helm 3.x for templating
- **Reconciliation**: FluxCD v2.x (HelmRelease + Kustomization only)
- **Cert Management**: cert-manager for TLS automation
- **Ingress**: NGINX Ingress Controller (preferred)

### Security Components
- **Identity Provider**: Keycloak 24+ (OIDC provider, no alternatives)
- **Secrets Management**: External Secrets Operator + cloud provider vault (e.g., Azure Key Vault)
- **Network Security**: NetworkPolicy + service mesh (optional for advanced use cases)

## Development Workflow

### Code Review & Quality Gates
1. **Feature Branch**: All work on feature branches following naming convention: `[###-feature-name]`
2. **Specification First**: Every feature requires `/specs/[###-feature]/spec.md` before implementation
3. **Tests Before Code**: TDD enforced; red-green-refactor cycle required (see Constitution Check below)
4. **Contract Testing**: Service contracts defined in `/specs/[###-feature]/contracts/` and validated before merge
5. **Code Review**: Minimum one approval required; reviewer must verify Constitution compliance

### Constitution Check (Mandatory Gate)
Before implementing any feature, verify:

- ✅ **Architecture**: Feature respects microservices boundaries; no monolithic additions
- ✅ **Security**: Authentication/authorization uses Keycloak Client Credentials or Authorization Code; no custom auth
- ✅ **Observability**: Structured logging, metrics, and traces configured; passed to LGTM
- ✅ **Configuration**: All environment-specific settings externalized as environment variables
- ✅ **Testing**: Acceptance criteria defined; at least one integration test included
- ✅ **Deployment**: Helm chart and values.yaml template prepared; FluxCD integration ready

### Testing Levels
1. **Unit Tests**: <100ms execution; mock all external dependencies
2. **Integration Tests**: <1 second per test; use test containers for PostgreSQL, Keycloak stub
3. **Contract Tests**: Validate API schemas match specification
4. **End-to-End Tests**: Optional; full Kubernetes deployment validation (slower, run in CI/CD only)

## Deployment & Release Process

### Versioning Strategy
- **Semantic Versioning**: MAJOR.MINOR.PATCH
  - **MAJOR**: Breaking API changes, non-backwards-compatible schema migrations
  - **MINOR**: New features, non-breaking enhancements
  - **PATCH**: Bug fixes, security patches, documentation updates
- **Git Tags**: Every release tagged as `v[VERSION]` in Git
- **Container Images**: Tagged identically to Git tag

### Deployment Flow
1. Feature complete, all tests pass, code reviewed
2. Merge to `main` branch
3. GitOps: FluxCD detects change, renders Helm templates, applies to Kubernetes
4. Deployment status monitored via FluxCD; rollback to previous version if health checks fail

### Rollback Procedure
- Git history is source of truth; rollback via reverting commit + Git push
- FluxCD automatically applies previous state
- No manual kubectl edits; no infrastructure-level rollbacks required

## Governance

### Amendment Procedure

1. **Proposal**: Summarize amendment with rationale in GitHub issue or discussion
2. **Justification**: Document why current principle is insufficient; propose new language
3. **Validation**: Verify no contradictions with existing principles; identify cascading updates needed
4. **Approval**: Architecture lead approval + consensus from core team
5. **Documentation**: Update constitution + propagate changes to templates (plan, spec, tasks)
6. **Migration**: Define transition plan if amendment affects existing features
7. **Commit**: Merge via PR with detailed commit message including rationale

### Version Bumping
- **MAJOR**: Backwards-incompatible principle removal or redefinition → requires migration plan
- **MINOR**: New principle or significant expansion → all new features comply immediately
- **PATCH**: Clarifications, wording, typo fixes, non-semantic refinements → no compliance impact

### Compliance Review
- **Per-PR**: Constitution Check gate enforces compliance at code review time
- **Quarterly**: Architecture team reviews all features shipped in quarter for unexpected exceptions
- **Annually**: Constitution revalidated; assess alignment with Haven standards and emerging best practices

### Dispute Resolution
- Constitution supersedes all other project guidance
- Disputes about principle interpretation escalated to architecture lead
- Complexity violations (see plan.md complexity tracking) justified before merge; unjustified violations blocked

### Runtime Development Guidance
- See `.github/copilot-instructions.md` for agent-specific guidance and current priorities
- See `README.md` for architecture overviews and command examples
- LGTM dashboards: hyperlinks in Grafana for system observability

---

**Version**: 1.0.0 | **Ratified**: 2026-05-05 | **Last Amended**: 2026-05-05

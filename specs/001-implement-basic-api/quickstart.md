# Quickstart Guide: Service Integration with OIDC Protection

**Feature**: Implement basic services with OIDC-protected communication  
**Date**: May 5, 2026  

## Overview

This feature implements two Spring Boot microservices:
- **Service A**: Gateway service exposing external REST API at `/api/info/{id}`
- **Service B**: Data service querying PostgreSQL for entity information

Services communicate via OIDC Client Credentials Flow using Keycloak.

## Prerequisites

- Java 25
- Gradle 8.x
- Docker & Docker Compose
- Kubernetes cluster (AKS) with FluxCD
- PostgreSQL 15+
- Keycloak 24+
- External Secrets Operator
- Terraform 1.x

## Local Development Setup

### 1. Clone and Setup

```bash
git clone <repository>
cd haven-poc-sdd
git checkout 001-implement-basic-services
```

### 2. Start Infrastructure

```bash
# Start PostgreSQL and Keycloak with Docker Compose
docker-compose up -d postgres keycloak
```

### 3. Configure Keycloak

- Access Keycloak at http://localhost:8080
- Create realm, client for service-a with client credentials
- Note client ID and secret

### 4. Run Services Locally

```bash
# Service B
cd apps/service-b
./gradlew bootRun

# Service A (in another terminal)
cd apps/service-a
./gradlew bootRun
```

### 5. Test API

```bash
# Get entity info
curl http://localhost:8080/api/info/123e4567-e89b-12d3-a456-426614174000
```

## Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

Tests use Testcontainers for PostgreSQL and Keycloak.

## Deployment

### 1. Infrastructure with Terraform

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

### 2. Deploy via FluxCD

```bash
# FluxCD will automatically deploy from Git
kubectl get helmreleases -n flux-system
```

### 3. Verify Deployment

```bash
# Check pods
kubectl get pods -n default

# Check services
kubectl get svc service-a service-b
```

## API Usage

### Get Entity Info

```bash
curl -X GET "https://your-domain/api/info/{entity-id}" \
  -H "Content-Type: application/json"
```

**Response**:
```json
{
  "name": "Sample Entity",
  "description": "Entity description",
  "version": "1.0.0"
}
```

## Monitoring

- **Logs**: Loki at `/loki`
- **Metrics**: Prometheus at `/prometheus`
- **Traces**: Tempo at `/tempo`
- **Dashboards**: Grafana at `/grafana`

## Troubleshooting

- Check health endpoints: `/health/live` and `/health/ready`
- View structured logs in Loki
- Monitor traces in Tempo
- Check Keycloak token issuance

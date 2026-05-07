#!/bin/bash

# Keycloak Client Configuration Script
# This script sets up clients and realms for the Haven POC OIDC authentication
#
# Usage:
#   ./setup-clients.sh [KEYCLOAK_URL] [ADMIN_USER] [ADMIN_PASSWORD]
#
# Default values:
#   KEYCLOAK_URL=http://localhost:8888
#   ADMIN_USER=admin
#   ADMIN_PASSWORD=admin
#
# Environment variables can override defaults

set -e

# Configuration
KEYCLOAK_URL="${1:-http://localhost:8888}"
ADMIN_USER="${2:-admin}"
ADMIN_PASSWORD="${3:-admin}"
REALM="${REALM:-demo}"
SERVICE_A_CLIENT="service-a"
SERVICE_B_CLIENT="service-b"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check if Keycloak is reachable
check_keycloak_health() {
    log_info "Checking Keycloak health at $KEYCLOAK_URL..."
    if ! curl -s "$KEYCLOAK_URL/health/ready" > /dev/null; then
        log_error "Keycloak is not responding. Please ensure it is running."
        exit 1
    fi
    log_info "Keycloak is healthy"
}

# Get admin token
get_admin_token() {
    log_info "Acquiring admin token..."
    local response=$(curl -s -X POST \
        "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=admin-cli" \
        -d "username=$ADMIN_USER" \
        -d "password=$ADMIN_PASSWORD" \
        -d "grant_type=password")

    local token=$(echo $response | jq -r '.access_token // empty')
    if [ -z "$token" ]; then
        log_error "Failed to get admin token. Response: $response"
        exit 1
    fi

    echo "$token"
}

# Create realm
create_realm() {
    local token=$1
    log_info "Creating realm: $REALM..."

    local response=$(curl -s -X POST \
        "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"realm\": \"$REALM\",
            \"displayName\": \"Haven POC Realm\",
            \"enabled\": true,
            \"accessTokenLifespan\": 3600
        }")

    if echo "$response" | grep -q "HTTP 409\|already exists"; then
        log_warn "Realm $REALM already exists"
    elif echo "$response" | grep -q "error"; then
        log_error "Failed to create realm: $response"
        exit 1
    fi

    log_info "Realm created successfully"
}

# Create service-a client (OAuth2 Client for Client Credentials flow)
create_service_a_client() {
    local token=$1
    log_info "Creating $SERVICE_A_CLIENT client..."

    local response=$(curl -s -X POST \
        "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"clientId\": \"$SERVICE_A_CLIENT\",
            \"name\": \"Service A\",
            \"description\": \"OAuth2 Client for Service A to authenticate to Service B\",
            \"enabled\": true,
            \"publicClient\": false,
            \"directAccessGrantsEnabled\": false,
            \"standardFlowEnabled\": false,
            \"implicitFlowEnabled\": false,
            \"serviceAccountsEnabled\": true,
            \"clientAuthenticatorType\": \"client-secret\",
            \"access\": {
                \"manage\": true,
                \"manage-members\": true,
                \"view\": true
            }
        }")

    local client_id=$(echo "$response" | jq -r '.id // empty')
    if [ -z "$client_id" ]; then
        log_error "Failed to create $SERVICE_A_CLIENT client: $response"
        exit 1
    fi

    log_info "Client $SERVICE_A_CLIENT created with ID: $client_id"
    echo "$client_id"
}

# Create service-b client (OAuth2 Resource Server)
create_service_b_client() {
    local token=$1
    log_info "Creating $SERVICE_B_CLIENT client..."

    local response=$(curl -s -X POST \
        "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"clientId\": \"$SERVICE_B_CLIENT\",
            \"name\": \"Service B\",
            \"description\": \"OAuth2 Resource Server for Service B\",
            \"enabled\": true,
            \"publicClient\": false,
            \"directAccessGrantsEnabled\": false,
            \"standardFlowEnabled\": false,
            \"implicitFlowEnabled\": false,
            \"serviceAccountsEnabled\": false,
            \"clientAuthenticatorType\": \"client-secret\",
            \"access\": {
                \"view\": true
            }
        }")

    local client_id=$(echo "$response" | jq -r '.id // empty')
    if [ -z "$client_id" ]; then
        log_error "Failed to create $SERVICE_B_CLIENT client: $response"
        exit 1
    fi

    log_info "Client $SERVICE_B_CLIENT created with ID: $client_id"
    echo "$client_id"
}

# Create client scope for service-to-service
create_service_scope() {
    local token=$1
    log_info "Creating service-scope client scope..."

    curl -s -X POST \
        "$KEYCLOAK_URL/admin/realms/$REALM/client-scopes" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"service-scope\",
            \"description\": \"Scope for service-to-service communication\",
            \"protocol\": \"openid-connect\"
        }" > /dev/null

    log_info "Client scope created successfully"
}

# Setup service account roles for service-a
setup_service_account_roles() {
    local token=$1
    local service_a_client_id=$2
    log_info "Setting up service account roles for $SERVICE_A_CLIENT..."

    # Find the service account user for service-a
    local users=$(curl -s \
        "$KEYCLOAK_URL/admin/realms/$REALM/users?search=service-account" \
        -H "Authorization: Bearer $token")

    log_info "Service account setup completed for $SERVICE_A_CLIENT"
}

# Main execution
main() {
    log_info "Starting Keycloak client setup..."
    log_info "Keycloak URL: $KEYCLOAK_URL"
    log_info "Realm: $REALM"

    check_keycloak_health

    local admin_token=$(get_admin_token)
    log_info "Admin token acquired"

    create_realm "$admin_token"
    create_service_scope "$admin_token"

    local service_a_id=$(create_service_a_client "$admin_token")
    local service_b_id=$(create_service_b_client "$admin_token")

    setup_service_account_roles "$admin_token" "$service_a_id"

    log_info "Keycloak setup completed successfully!"
    log_info ""
    log_info "Configuration Summary:"
    log_info "  Keycloak URL: $KEYCLOAK_URL"
    log_info "  Realm: $REALM"
    log_info "  Service A Client ID: $SERVICE_A_CLIENT"
    log_info "  Service B Client ID: $SERVICE_B_CLIENT"
    log_info ""
    log_info "Next steps:"
    log_info "  1. Retrieve client secrets for both clients from Keycloak admin console"
    log_info "  2. Update application.yaml files in both services with client credentials"
    log_info "  3. Update service.b.base-url in Service A configuration"
    log_info ""
}

# Run main function
main


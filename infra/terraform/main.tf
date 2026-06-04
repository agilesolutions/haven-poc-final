terraform {
  required_version = ">= 1.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "main" {
  name                = var.cluster_name
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = var.cluster_name

  default_node_pool {
    name       = "default"
    node_count = var.node_count
    vm_size    = var.vm_size

    os_disk_size_gb = 30

    max_pods = 110
  }

  service_principal {
    client_id     = var.client_id
    client_secret = var.client_secret
  }

  network_profile {
    network_plugin    = "azure"
    network_policy    = "azure"
    dns_service_ip    = "10.0.0.10"
    docker_bridge_cidr = "172.17.0.1/16"
    service_cidr      = "10.0.0.0/16"
  }

  role_based_access_control_enabled = true

  depends_on = [azurerm_resource_group.main]
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "main" {
  name                   = var.postgres_server_name
  location               = azurerm_resource_group.main.location
  resource_group_name    = azurerm_resource_group.main.name
  administrator_login    = var.postgres_admin_login
  administrator_password = var.postgres_admin_password
  version                = "14"
  zone                   = 1

  storage_mb   = 32768
  sku_name     = "B_Standard_B2s"

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  auto_grow_enabled            = false

  high_availability {
    mode = "ZoneRedundant"
  }

  depends_on = [azurerm_resource_group.main]
}

# PostgreSQL Database
resource "azurerm_postgresql_flexible_server_database" "service_a_db" {
  name             = "service_a_db"
  server_id        = azurerm_postgresql_flexible_server.main.id
  collation        = "en_US.utf8"
  charset          = "UTF8"

  depends_on = [azurerm_postgresql_flexible_server.main]
}

resource "azurerm_postgresql_flexible_server_database" "service_b_db" {
  name             = "service_b_db"
  server_id        = azurerm_postgresql_flexible_server.main.id
  collation        = "en_US.utf8"
  charset          = "UTF8"

  depends_on = [azurerm_postgresql_flexible_server.main]
}

# PostgreSQL Firewall Rule for AKS
resource "azurerm_postgresql_flexible_server_firewall_rule" "aks" {
  name             = "aks-cluster"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "255.255.255.255"
}

# Outputs
output "aks_cluster_id" {
  value       = azurerm_kubernetes_cluster.main.id
  description = "AKS Cluster ID"
}

output "aks_kube_config" {
  value       = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive   = true
  description = "Kubernetes configuration"
}

output "postgres_server_fqdn" {
  value       = azurerm_postgresql_flexible_server.main.fqdn
  description = "PostgreSQL server FQDN"
}

output "postgres_server_id" {
  value       = azurerm_postgresql_flexible_server.main.id
  description = "PostgreSQL server ID"
}

output "service_a_database_name" {
  value       = azurerm_postgresql_flexible_server_database.service_a_db.name
  description = "Service A database name"
}

output "service_b_database_name" {
  value       = azurerm_postgresql_flexible_server_database.service_b_db.name
  description = "Service B database name"
}



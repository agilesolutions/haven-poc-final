output "aks_cluster_id" {
  description = "AKS Cluster Resource ID"
  value       = azurerm_kubernetes_cluster.main.id
}

output "aks_cluster_name" {
  description = "AKS Cluster Name"
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_fqdn" {
  description = "AKS cluster FQDN"
  value       = azurerm_kubernetes_cluster.main.fqdn
}

output "kube_config" {
  description = "Raw kube config"
  value       = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive   = true
}

output "kube_config_context" {
  description = "Kube config context"
  value       = azurerm_kubernetes_cluster.main.kube_config[0].current_context
}

output "resource_group_id" {
  description = "Resource Group ID"
  value       = azurerm_resource_group.main.id
}

output "postgres_server_fqdn" {
  description = "PostgreSQL Flexible Server FQDN"
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "postgres_server_name" {
  description = "PostgreSQL Flexible Server Name"
  value       = azurerm_postgresql_flexible_server.main.name
}

output "postgres_admin_login" {
  description = "PostgreSQL Administrator Login"
  value       = azurerm_postgresql_flexible_server.main.administrator_login
  sensitive   = true
}

output "service_a_database_name" {
  description = "Service A PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.service_a_db.name
}

output "service_b_database_name" {
  description = "Service B PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.service_b_db.name
}

output "postgres_connection_string_service_a" {
  description = "PostgreSQL Connection String for Service A"
  value       = "postgresql://${azurerm_postgresql_flexible_server.main.administrator_login}@${azurerm_postgresql_flexible_server.main.name}:${azurerm_postgresql_flexible_server_database.service_a_db.name}@${azurerm_postgresql_flexible_server.main.fqdn}:5432"
  sensitive   = true
}

output "postgres_connection_string_service_b" {
  description = "PostgreSQL Connection String for Service B"
  value       = "postgresql://${azurerm_postgresql_flexible_server.main.administrator_login}@${azurerm_postgresql_flexible_server.main.name}:${azurerm_postgresql_flexible_server_database.service_b_db.name}@${azurerm_postgresql_flexible_server.main.fqdn}:5432"
  sensitive   = true
}

output "kubeconfig_commands" {
  description = "Commands to configure kubectl"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${azurerm_kubernetes_cluster.main.name}"
}



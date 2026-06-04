variable "resource_group_name" {
  description = "Name of the Azure resource group"
  type        = string
  default     = "haven-poc-rg"
}

variable "location" {
  description = "Azure region/location"
  type        = string
  default     = "East US"
}

variable "cluster_name" {
  description = "Name of the AKS cluster"
  type        = string
  default     = "haven-poc-aks"
}

variable "node_count" {
  description = "Number of nodes in the AKS cluster"
  type        = number
  default     = 2
}

variable "vm_size" {
  description = "VM size for AKS nodes"
  type        = string
  default     = "Standard_D2s_v3"
}

variable "client_id" {
  description = "Service Principal Client ID for AKS"
  type        = string
  sensitive   = true
}

variable "client_secret" {
  description = "Service Principal Client Secret for AKS"
  type        = string
  sensitive   = true
}

variable "postgres_server_name" {
  description = "PostgreSQL Flexible Server name"
  type        = string
  default     = "haven-poc-postgres"
}

variable "postgres_admin_login" {
  description = "PostgreSQL administrator login"
  type        = string
  default     = "pgadmin"
  sensitive   = true
}

variable "postgres_admin_password" {
  description = "PostgreSQL administrator password"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "tags" {
  description = "Common tags to apply to resources"
  type        = map(string)
  default = {
    Environment = "dev"
    Project     = "haven-poc"
    Team        = "platform"
  }
}



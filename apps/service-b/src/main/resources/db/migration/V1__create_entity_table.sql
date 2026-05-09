-- V1__create_entity_table.sql
-- Initial schema creation for entity table
-- Created: May 8, 2026

-- Create entity table with all required fields
CREATE TABLE IF NOT EXISTS entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Create indexes for common query patterns
CREATE INDEX idx_entity_name ON entity(name);
CREATE INDEX idx_entity_active ON entity(is_active);
CREATE INDEX idx_entity_created_at ON entity(created_at);
CREATE INDEX idx_entity_updated_at ON entity(updated_at);

-- Create composite index for frequently used queries
CREATE INDEX idx_entity_active_created_at ON entity(is_active, created_at DESC);

-- Create index for querying active entities by name
CREATE INDEX idx_entity_active_name ON entity(is_active, name);

-- Create index for lookup performance
CREATE UNIQUE INDEX idx_entity_name_unique ON entity(name) WHERE is_active = true;

-- Add constraints with meaningful names
ALTER TABLE entity ADD CONSTRAINT chk_entity_name_not_empty CHECK (name <> '');
ALTER TABLE entity ADD CONSTRAINT chk_entity_version_not_empty CHECK (version <> '');
ALTER TABLE entity ADD CONSTRAINT chk_entity_name_length CHECK (char_length(name) <= 255);
ALTER TABLE entity ADD CONSTRAINT chk_entity_version_length CHECK (char_length(version) <= 50);

-- Add comment for documentation
COMMENT ON TABLE entity IS 'Core entity information table for system data';
COMMENT ON COLUMN entity.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN entity.name IS 'Entity name, must be unique and non-empty';
COMMENT ON COLUMN entity.description IS 'Optional detailed description of the entity';
COMMENT ON COLUMN entity.version IS 'Version string following semantic versioning';
COMMENT ON COLUMN entity.created_at IS 'Timestamp when entity was created';
COMMENT ON COLUMN entity.updated_at IS 'Timestamp when entity was last updated';
COMMENT ON COLUMN entity.created_by IS 'User who created this entity';
COMMENT ON COLUMN entity.updated_by IS 'User who last updated this entity';
COMMENT ON COLUMN entity.is_active IS 'Flag indicating if entity is active/deleted';


-- Create Entity table for Service B
-- This table stores entity information retrieved by external callers

CREATE TABLE IF NOT EXISTS entity (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  is_active BOOLEAN DEFAULT true,

  CONSTRAINT entity_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
  CONSTRAINT entity_version_not_empty CHECK (LENGTH(TRIM(version)) > 0)
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_entity_name ON entity(name);
CREATE INDEX IF NOT EXISTS idx_entity_active ON entity(is_active);
CREATE INDEX IF NOT EXISTS idx_entity_created_at ON entity(created_at);
CREATE INDEX IF NOT EXISTS idx_entity_updated_at ON entity(updated_at);

-- Create info table for backward compatibility (if needed for other purposes)
CREATE TABLE IF NOT EXISTS info (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  version TEXT DEFAULT '1.0.0',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT info_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- Create indexes for info table
CREATE INDEX IF NOT EXISTS idx_info_name ON info(name);
CREATE INDEX IF NOT EXISTS idx_info_updated_at ON info(updated_at);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for entity table
DROP TRIGGER IF EXISTS update_entity_updated_at ON entity;
CREATE TRIGGER update_entity_updated_at BEFORE UPDATE ON entity
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create trigger for info table
DROP TRIGGER IF EXISTS update_info_updated_at ON info;
CREATE TRIGGER update_info_updated_at BEFORE UPDATE ON info
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


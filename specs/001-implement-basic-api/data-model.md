# Data Model

## Entities

### Entity

Represents domain information with name, description, and version attributes.

**Fields**:
- `id`: UUID (primary key, auto-generated for uniqueness)
- `name`: String (required, unique identifier)
- `description`: String (required, descriptive text)
- `version`: String (required, version number in semantic format)

**Relationships**: None

**Validation Rules**:
- `name`: Not null, not empty, maximum 255 characters, unique
- `description`: Not null, not empty
- `version`: Not null, matches semantic version pattern (e.g., MAJOR.MINOR.PATCH)

**State Transitions**: None (static entity with no lifecycle states)

**Database Schema**:
```sql
CREATE TABLE entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    version VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX idx_entity_name ON entity(name);
```

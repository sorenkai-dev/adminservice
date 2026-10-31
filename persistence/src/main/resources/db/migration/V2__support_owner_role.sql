-- V2: Update role_assignments check constraint to include OWNER role

ALTER TABLE role_assignments DROP CONSTRAINT IF EXISTS chk_role_valid;
ALTER TABLE role_assignments ADD CONSTRAINT chk_role_valid CHECK (role IN ('MODERATOR', 'ADMIN', 'OWNER'));

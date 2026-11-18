-- Run once before deploying user management. Existing accounts stay active.
ALTER TABLE users ADD COLUMN status ENUM('active','locked') NOT NULL DEFAULT 'active';

-- ============================================================
-- UPDATE USER PASSWORDS
-- Fix password hashes for all test users
-- ============================================================

USE hirely_db;

-- Update all users with correct BCrypt hash for 'password123'
-- Generated using BCrypt with cost factor 10

UPDATE users SET password_hash = '$2a$10$vsEvgu95.jurbolXWkkqqu6f1/Ri/e5/DBw.pvjpxecI9D4Wx5W96'
WHERE email IN (
    'admin@hirely.com',
    'recruiter@company.com',
    'jane.candidate@email.com',
    'john.doe@email.com',
    'jane.smith@email.com',
    'mike.chen@email.com',
    'sarah.wilson@email.com'
);

-- Verify the update
SELECT
    email,
    SUBSTRING(password_hash, 1, 20) AS hash_preview,
    is_active,
    last_login
FROM users
ORDER BY user_id;

SELECT 'Password hashes updated successfully!' AS Status;

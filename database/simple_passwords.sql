-- Update all users with plain text password 'password123'

USE hirely_db;

UPDATE users SET password_hash = 'password123' WHERE email = 'admin@hirely.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'recruiter@company.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'jane.candidate@email.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'john.doe@email.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'jane.smith@email.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'mike.chen@email.com';
UPDATE users SET password_hash = 'password123' WHERE email = 'sarah.wilson@email.com';

SELECT 'Passwords updated to plain text!' AS Status;

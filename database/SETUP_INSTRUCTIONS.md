# Database Setup Instructions

## Prerequisites
- WAMP or XAMPP installed and running
- MySQL service started
- phpMyAdmin accessible at http://localhost/phpmyadmin

## Option 1: Import via phpMyAdmin (Recommended)

### Step 1: Start WAMP/XAMPP
- Make sure Apache and MySQL services are running (green icons)

### Step 2: Open phpMyAdmin
- Navigate to: http://localhost/phpmyadmin
- Login with:
  - Username: `root`
  - Password: (leave empty or use your configured password)

### Step 3: Import the Schema
1. Click on the "Import" tab at the top
2. Click "Choose File" and select: `hirely_database_schema.sql`
3. Click "Go" at the bottom of the page
4. Wait for the import to complete (should take a few seconds)

### Step 4: Verify Database
- You should see `hirely_db` in the left sidebar
- Click on it to expand and see all tables
- Expected tables: 18 tables + 3 views

## Option 2: Import via MySQL Command Line

```bash
# Navigate to the database folder
cd "C:\Users\rouk1\OneDrive\Bureau\interview_evaluation\database"

# Import the schema (adjust path to mysql.exe if needed)
# For WAMP: C:\wamp64\bin\mysql\mysql8.x.x\bin\mysql.exe
# For XAMPP: C:\xampp\mysql\bin\mysql.exe

mysql -u root -p < hirely_database_schema.sql
```

## What Gets Created

### Database Structure
- **Database Name:** `hirely_db`
- **Character Set:** utf8mb4
- **Collation:** utf8mb4_unicode_ci

### Tables (18 total)
1. **Authentication & Users:**
   - roles
   - users
   - recruiter_profiles
   - interviewee_profiles

2. **Job Offers & Applications:** (Mock data - managed by other team)
   - job_offers
   - applications

3. **Interview Management:** (Your primary focus)
   - interview_types
   - interviews
   - interview_responses
   - recruiter_availability
   - recruiter_unavailability

4. **Evaluation System:** (Your primary focus)
   - evaluation_criteria
   - interview_evaluations
   - evaluation_scores

5. **Supporting Tables:**
   - notifications
   - audit_logs

### Views (3 total)
- `upcoming_interviews` - Shows all upcoming scheduled interviews with details
- `evaluation_summary` - Summary of all completed evaluations
- `candidate_progress` - Tracks candidate progress through interview pipeline

### Pre-loaded Data

#### Test Users (All passwords: `password123`)
| Role | Email | Name | Purpose |
|------|-------|------|---------|
| ADMIN | admin@hirely.com | Admin User | System administration |
| RECRUITER | recruiter@company.com | Sarah Johnson | HR Recruiter |
| INTERVIEWEE | jane.candidate@email.com | Jane Candidate | Test candidate 1 |
| INTERVIEWEE | john.doe@email.com | John Doe | Test candidate 2 |
| INTERVIEWEE | jane.smith@email.com | Jane Smith | Test candidate 3 |
| INTERVIEWEE | mike.chen@email.com | Mike Chen | Test candidate 4 |
| INTERVIEWEE | sarah.wilson@email.com | Sarah Wilson | Test candidate 5 |

#### Mock Job Offers (4 jobs)
1. Senior Java Developer (Remote)
2. Frontend React Developer (New York, NY)
3. Python Backend Engineer (San Francisco, CA)
4. UX/UI Designer (Remote)

#### Mock Applications (8 applications ready for interview)
- 4 applications in SHORTLISTED status (ready to schedule interviews)
- 4 applications in UNDER_REVIEW status

#### Reference Data
- **6 Interview Types:** Phone Screening, Video Interview, In-Person, Technical Interview, Panel Interview, Final Interview
- **8 Evaluation Criteria:** Technical Skills, Problem Solving, Communication, Cultural Fit, Experience, Motivation, Leadership, Adaptability

## Verification Queries

After importing, run these queries in phpMyAdmin to verify:

```sql
-- Check if database was created
SHOW DATABASES LIKE 'hirely_db';

-- Use the database
USE hirely_db;

-- Count tables
SELECT COUNT(*) AS total_tables FROM information_schema.tables
WHERE table_schema = 'hirely_db' AND table_type = 'BASE TABLE';
-- Expected: 18

-- Check test users
SELECT u.email, r.role_name,
       CASE
           WHEN r.role_name = 'RECRUITER' THEN CONCAT(rp.first_name, ' ', rp.last_name)
           WHEN r.role_name = 'INTERVIEWEE' THEN CONCAT(ip.first_name, ' ', ip.last_name)
           ELSE 'Admin User'
       END AS full_name
FROM users u
JOIN roles r ON u.role_id = r.role_id
LEFT JOIN recruiter_profiles rp ON u.user_id = rp.user_id
LEFT JOIN interviewee_profiles ip ON u.user_id = ip.user_id;
-- Expected: 7 users

-- Check job offers
SELECT title, location, status FROM job_offers;
-- Expected: 4 job offers

-- Check applications ready for interview
SELECT
    jo.title AS job_title,
    CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name,
    app.status,
    app.applied_date
FROM applications app
JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id
JOIN interviewee_profiles ip ON app.interviewee_id = ip.interviewee_id
ORDER BY app.applied_date DESC;
-- Expected: 8 applications

-- Check interview types
SELECT type_name, typical_duration_minutes FROM interview_types;
-- Expected: 6 types

-- Check evaluation criteria
SELECT criteria_name, max_score, weight FROM evaluation_criteria ORDER BY display_order;
-- Expected: 8 criteria
```

## Troubleshooting

### Error: "Table already exists"
- The schema file drops and recreates the database
- If you get this error, either:
  1. Delete `hirely_db` manually and re-import
  2. Or check if the import actually succeeded

### Error: "Access denied"
- Check your MySQL root password
- Default for WAMP/XAMPP is usually empty password

### Error: "Unknown database"
- Make sure you selected the correct SQL file
- The file should start with `DROP DATABASE IF EXISTS hirely_db;`

### Cannot see database in phpMyAdmin
- Click "Refresh" icon in the left sidebar
- Or refresh the page

## Next Steps

After successful database setup:
1. ✅ Test database connection in your Java application
2. ✅ Verify you can login with test credentials
3. ✅ Start building the JavaFX interface
4. ✅ Implement authentication module

## Database Connection String

Use this in your Java application:

```java
String url = "jdbc:mysql://localhost:3306/hirely_db?useSSL=false&serverTimezone=UTC";
String username = "root";
String password = ""; // Empty for default WAMP/XAMPP, or your configured password
```

## Support

If you encounter any issues:
1. Check that MySQL service is running
2. Verify phpMyAdmin is accessible
3. Check the import log for specific error messages
4. Make sure you have proper permissions to create databases

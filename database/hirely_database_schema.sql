-- ============================================================
-- HIRELY DATABASE SCHEMA
-- Interview & Evaluation Module
-- Database: hirely_db
-- Last Updated: 2026-02-16
-- ============================================================

-- Drop database if exists and create fresh
DROP DATABASE IF EXISTS hirely_db;
CREATE DATABASE hirely_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hirely_db;

-- ============================================================
-- SECTION 1: USER AUTHENTICATION & ROLES
-- ============================================================

-- Roles table (ADMIN, RECRUITER, INTERVIEWEE)
CREATE TABLE roles (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    role_name ENUM('ADMIN', 'RECRUITER', 'INTERVIEWEE') NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Main users table (authentication)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT,
    INDEX idx_email (email),
    INDEX idx_role (role_id)
);

-- Recruiter profiles
CREATE TABLE recruiter_profiles (
    recruiter_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(255),
    position VARCHAR(100),
    phone_number VARCHAR(20),
    department VARCHAR(100),
    bio TEXT,
    profile_picture_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Interviewee (candidate) profiles
CREATE TABLE interviewee_profiles (
    interviewee_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    resume_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    portfolio_url VARCHAR(500),
    skills TEXT,
    experience_years INT DEFAULT 0,
    education TEXT,
    bio TEXT,
    profile_picture_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- SECTION 2: JOB OFFERS & APPLICATIONS (Other Team - Mock Data)
-- ============================================================

-- Job offers table (managed by other team members)
CREATE TABLE job_offers (
    job_offer_id INT PRIMARY KEY AUTO_INCREMENT,
    recruiter_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requirements TEXT,
    location VARCHAR(255),
    job_type ENUM('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP') DEFAULT 'FULL_TIME',
    salary_range VARCHAR(100),
    experience_required INT DEFAULT 0,
    status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') DEFAULT 'DRAFT',
    posted_date DATE,
    closing_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles(recruiter_id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_recruiter (recruiter_id)
);

-- Applications table (candidates applying to jobs)
CREATE TABLE applications (
    application_id INT PRIMARY KEY AUTO_INCREMENT,
    job_offer_id INT NOT NULL,
    interviewee_id INT NOT NULL,
    cover_letter TEXT,
    status ENUM('SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED',
                'INTERVIEW_SCHEDULED', 'INTERVIEWED', 'REJECTED', 'ACCEPTED') DEFAULT 'SUBMITTED',
    applied_date DATE NOT NULL,
    reviewed_date DATE NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(job_offer_id) ON DELETE CASCADE,
    FOREIGN KEY (interviewee_id) REFERENCES interviewee_profiles(interviewee_id) ON DELETE CASCADE,
    UNIQUE KEY unique_application (job_offer_id, interviewee_id),
    INDEX idx_status (status),
    INDEX idx_interviewee (interviewee_id)
);

-- ============================================================
-- SECTION 3: INTERVIEW MODULE (YOUR PRIMARY FOCUS)
-- ============================================================

-- Interview types (Phone, Video, In-Person, Technical, Panel, Final)
CREATE TABLE interview_types (
    interview_type_id INT PRIMARY KEY AUTO_INCREMENT,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    typical_duration_minutes INT DEFAULT 60,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Main interviews table
CREATE TABLE interviews (
    interview_id INT PRIMARY KEY AUTO_INCREMENT,
    application_id INT NOT NULL,
    recruiter_id INT NOT NULL,
    interviewee_id INT NOT NULL,
    interview_type_id INT NOT NULL,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    location VARCHAR(255) NULL,
    meeting_link VARCHAR(500) NULL,
    status ENUM('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'RESCHEDULED', 'COMPLETED', 'NO_SHOW') DEFAULT 'SCHEDULED',
    interview_round INT DEFAULT 1,
    notes TEXT,
    cancellation_reason TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles(recruiter_id) ON DELETE CASCADE,
    FOREIGN KEY (interviewee_id) REFERENCES interviewee_profiles(interviewee_id) ON DELETE CASCADE,
    FOREIGN KEY (interview_type_id) REFERENCES interview_types(interview_type_id) ON DELETE RESTRICT,
    INDEX idx_scheduled_date (scheduled_date),
    INDEX idx_status (status),
    INDEX idx_recruiter (recruiter_id),
    INDEX idx_interviewee (interviewee_id)
);

-- Interview responses (Accept/Decline by candidate)
CREATE TABLE interview_responses (
    response_id INT PRIMARY KEY AUTO_INCREMENT,
    interview_id INT NOT NULL,
    response_type ENUM('ACCEPTED', 'DECLINED', 'RESCHEDULE_REQUESTED') NOT NULL,
    response_message TEXT,
    preferred_dates TEXT NULL,
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (interview_id) REFERENCES interviews(interview_id) ON DELETE CASCADE,
    INDEX idx_interview (interview_id)
);

-- Recruiter availability (optional feature)
CREATE TABLE recruiter_availability (
    availability_id INT PRIMARY KEY AUTO_INCREMENT,
    recruiter_id INT NOT NULL,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles(recruiter_id) ON DELETE CASCADE
);

-- Recruiter unavailability (time off, meetings)
CREATE TABLE recruiter_unavailability (
    unavailability_id INT PRIMARY KEY AUTO_INCREMENT,
    recruiter_id INT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles(recruiter_id) ON DELETE CASCADE
);

-- ============================================================
-- SECTION 4: EVALUATION MODULE (YOUR PRIMARY FOCUS)
-- ============================================================

-- Evaluation criteria (Technical Skills, Communication, etc.)
CREATE TABLE evaluation_criteria (
    criteria_id INT PRIMARY KEY AUTO_INCREMENT,
    criteria_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    max_score INT DEFAULT 5,
    weight DECIMAL(3, 2) DEFAULT 1.00,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Interview evaluations (overall assessment)
CREATE TABLE interview_evaluations (
    evaluation_id INT PRIMARY KEY AUTO_INCREMENT,
    interview_id INT NOT NULL UNIQUE,
    recruiter_id INT NOT NULL,
    overall_rating DECIMAL(3, 2) NOT NULL,
    recommendation ENUM('STRONGLY_RECOMMEND', 'RECOMMEND', 'NEUTRAL', 'NOT_RECOMMEND', 'STRONGLY_NOT_RECOMMEND') NOT NULL,
    strengths TEXT,
    weaknesses TEXT,
    general_comments TEXT,
    hire_decision ENUM('HIRE', 'NO_HIRE', 'MAYBE', 'PENDING') DEFAULT 'PENDING',
    next_steps TEXT,
    is_draft BOOLEAN DEFAULT FALSE,
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (interview_id) REFERENCES interviews(interview_id) ON DELETE CASCADE,
    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles(recruiter_id) ON DELETE CASCADE,
    INDEX idx_hire_decision (hire_decision)
);

-- Detailed scores per criteria
CREATE TABLE evaluation_scores (
    score_id INT PRIMARY KEY AUTO_INCREMENT,
    evaluation_id INT NOT NULL,
    criteria_id INT NOT NULL,
    score INT NOT NULL,
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evaluation_id) REFERENCES interview_evaluations(evaluation_id) ON DELETE CASCADE,
    FOREIGN KEY (criteria_id) REFERENCES evaluation_criteria(criteria_id) ON DELETE CASCADE,
    UNIQUE KEY unique_eval_criteria (evaluation_id, criteria_id),
    CHECK (score >= 0 AND score <= 5)
);

-- ============================================================
-- SECTION 5: NOTIFICATIONS & AUDIT
-- ============================================================

-- Notifications table
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    interview_id INT NULL,
    notification_type ENUM('INTERVIEW_SCHEDULED', 'INTERVIEW_REMINDER',
                          'INTERVIEW_CONFIRMED', 'INTERVIEW_CANCELLED',
                          'INTERVIEW_RESCHEDULED', 'EVALUATION_COMPLETED',
                          'INTERVIEW_INVITATION', 'GENERAL') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (interview_id) REFERENCES interviews(interview_id) ON DELETE CASCADE,
    INDEX idx_user_unread (user_id, is_read),
    INDEX idx_created (created_at)
);

-- Audit logs (track important actions)
CREATE TABLE audit_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at)
);

-- ============================================================
-- SECTION 6: VIEWS (Useful Queries)
-- ============================================================

-- View: Upcoming interviews with all details
CREATE VIEW upcoming_interviews AS
SELECT
    i.interview_id,
    i.scheduled_date,
    i.scheduled_time,
    i.duration_minutes,
    i.status,
    it.type_name AS interview_type,
    CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name,
    ip.phone_number AS candidate_phone,
    u_interviewee.email AS candidate_email,
    CONCAT(rp.first_name, ' ', rp.last_name) AS recruiter_name,
    u_recruiter.email AS recruiter_email,
    jo.title AS job_title,
    jo.location AS job_location,
    i.meeting_link,
    i.location AS interview_location,
    i.notes
FROM interviews i
JOIN interview_types it ON i.interview_type_id = it.interview_type_id
JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id
JOIN users u_interviewee ON ip.user_id = u_interviewee.user_id
JOIN recruiter_profiles rp ON i.recruiter_id = rp.recruiter_id
JOIN users u_recruiter ON rp.user_id = u_recruiter.user_id
JOIN applications app ON i.application_id = app.application_id
JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id
WHERE i.scheduled_date >= CURDATE()
AND i.status IN ('SCHEDULED', 'CONFIRMED', 'RESCHEDULED')
ORDER BY i.scheduled_date, i.scheduled_time;

-- View: Evaluation summary
CREATE VIEW evaluation_summary AS
SELECT
    e.evaluation_id,
    i.interview_id,
    i.scheduled_date AS interview_date,
    CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name,
    jo.title AS job_title,
    e.overall_rating,
    e.recommendation,
    e.hire_decision,
    CONCAT(rp.first_name, ' ', rp.last_name) AS evaluated_by,
    e.evaluated_at
FROM interview_evaluations e
JOIN interviews i ON e.interview_id = i.interview_id
JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id
JOIN recruiter_profiles rp ON e.recruiter_id = rp.recruiter_id
JOIN applications app ON i.application_id = app.application_id
JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id
ORDER BY e.evaluated_at DESC;

-- View: Candidate application progress
CREATE VIEW candidate_progress AS
SELECT
    app.application_id,
    CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name,
    u.email AS candidate_email,
    jo.title AS job_title,
    app.status AS application_status,
    app.applied_date,
    COUNT(DISTINCT i.interview_id) AS total_interviews,
    SUM(CASE WHEN i.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_interviews,
    MAX(i.scheduled_date) AS last_interview_date,
    e.hire_decision
FROM applications app
JOIN interviewee_profiles ip ON app.interviewee_id = ip.interviewee_id
JOIN users u ON ip.user_id = u.user_id
JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id
LEFT JOIN interviews i ON app.application_id = i.application_id
LEFT JOIN interview_evaluations e ON i.interview_id = e.interview_id
GROUP BY app.application_id, ip.interviewee_id, u.email, jo.title, app.status, app.applied_date, e.hire_decision;

-- ============================================================
-- SECTION 7: INSERT DEFAULT DATA
-- ============================================================

-- Insert roles
INSERT INTO roles (role_name, description) VALUES
('ADMIN', 'System administrator with full access'),
('RECRUITER', 'HR recruiter who schedules and evaluates interviews'),
('INTERVIEWEE', 'Job candidate/applicant');

-- Insert interview types
INSERT INTO interview_types (type_name, description, typical_duration_minutes) VALUES
('PHONE_SCREENING', 'Initial phone screening interview', 30),
('VIDEO_INTERVIEW', 'Virtual video interview via online platform', 60),
('IN_PERSON', 'Face-to-face interview at company office', 60),
('TECHNICAL_INTERVIEW', 'Technical assessment and coding interview', 90),
('PANEL_INTERVIEW', 'Interview with multiple interviewers', 90),
('FINAL_INTERVIEW', 'Final round interview with senior management', 60);

-- Insert evaluation criteria
INSERT INTO evaluation_criteria (criteria_name, description, max_score, weight, category, display_order) VALUES
('Technical Skills', 'Proficiency in required technical skills and technologies', 5, 1.50, 'Technical', 1),
('Problem Solving', 'Ability to analyze and solve complex problems', 5, 1.30, 'Technical', 2),
('Communication', 'Clarity and effectiveness in verbal communication', 5, 1.20, 'Soft Skills', 3),
('Cultural Fit', 'Alignment with company values and work culture', 5, 1.00, 'Soft Skills', 4),
('Experience', 'Relevant work experience and achievements', 5, 1.10, 'Background', 5),
('Motivation', 'Enthusiasm and genuine interest in the role', 5, 1.00, 'Attitude', 6),
('Leadership', 'Leadership potential and team collaboration', 5, 0.90, 'Soft Skills', 7),
('Adaptability', 'Flexibility and ability to learn new things', 5, 1.00, 'Soft Skills', 8);

-- ============================================================
-- SECTION 8: INSERT TEST DATA (Users, Profiles, Mock Data)
-- ============================================================

-- Insert test users (plain text passwords for development)
-- Password: password123
INSERT INTO users (email, password_hash, role_id, is_active, is_verified, last_login) VALUES
-- Admin user
('admin@hirely.com', 'password123', 1, TRUE, TRUE, NOW()),
-- Recruiter user
('recruiter@company.com', 'password123', 2, TRUE, TRUE, NOW()),
-- Interviewee users
('jane.candidate@email.com', 'password123', 3, TRUE, TRUE, NULL),
('john.doe@email.com', 'password123', 3, TRUE, TRUE, NULL),
('jane.smith@email.com', 'password123', 3, TRUE, TRUE, NULL),
('mike.chen@email.com', 'password123', 3, TRUE, TRUE, NULL),
('sarah.wilson@email.com', 'password123', 3, TRUE, TRUE, NULL);

-- Insert recruiter profile
INSERT INTO recruiter_profiles (user_id, first_name, last_name, company_name, position, phone_number, department) VALUES
(2, 'Sarah', 'Johnson', 'Tech Innovations Inc.', 'Senior HR Recruiter', '+1-555-0123', 'Human Resources');

-- Insert interviewee profiles
INSERT INTO interviewee_profiles (user_id, first_name, last_name, phone_number, skills, experience_years, education) VALUES
(3, 'Jane', 'Candidate', '+1-555-1001', 'Java, Spring Boot, MySQL, JavaScript', 4, 'Bachelor of Computer Science'),
(4, 'John', 'Doe', '+1-555-1002', 'Python, Django, PostgreSQL, Docker', 5, 'Master of Software Engineering'),
(5, 'Jane', 'Smith', '+1-555-1003', 'React, TypeScript, Node.js, MongoDB', 3, 'Bachelor of Information Technology'),
(6, 'Mike', 'Chen', '+1-555-1004', 'Java, Microservices, AWS, Kubernetes', 7, 'Master of Computer Science'),
(7, 'Sarah', 'Wilson', '+1-555-1005', 'UI/UX Design, Figma, Adobe XD, HTML/CSS', 4, 'Bachelor of Design');

-- ============================================================
-- INSERT MOCK DATA (Job Offers & Applications) - Other Team
-- ============================================================

-- Insert mock job offers (static data - other team handles this)
INSERT INTO job_offers (recruiter_id, title, description, requirements, location, job_type, salary_range, experience_required, status, posted_date, closing_date) VALUES
(1, 'Senior Java Developer',
 'We are looking for an experienced Java developer to join our enterprise development team. You will be responsible for building scalable backend services and microservices.',
 'Strong knowledge of Java, Spring Boot, REST APIs, SQL databases. Experience with microservices architecture and cloud platforms.',
 'Remote',
 'FULL_TIME',
 '$90,000 - $120,000',
 5,
 'PUBLISHED',
 '2026-02-01',
 '2026-03-31'),

(1, 'Frontend React Developer',
 'Join our dynamic team to build modern web applications using React and TypeScript. You will work on customer-facing products with a focus on user experience.',
 'Proficiency in React, TypeScript, HTML/CSS. Experience with state management (Redux/Context API) and RESTful APIs.',
 'New York, NY',
 'FULL_TIME',
 '$80,000 - $110,000',
 3,
 'PUBLISHED',
 '2026-02-05',
 '2026-03-31'),

(1, 'Python Backend Engineer',
 'Build scalable APIs and backend services using Python and Django. Work with data pipelines and integrate with various third-party services.',
 'Strong Python skills, Django/Flask experience, RESTful API design, SQL and NoSQL databases.',
 'San Francisco, CA',
 'FULL_TIME',
 '$95,000 - $125,000',
 4,
 'PUBLISHED',
 '2026-02-08',
 '2026-03-31'),

(1, 'UX/UI Designer',
 'We need a creative UX/UI designer to craft beautiful and intuitive user interfaces for our SaaS products.',
 'Strong portfolio, proficiency in Figma/Adobe XD, understanding of user-centered design principles.',
 'Remote',
 'FULL_TIME',
 '$70,000 - $95,000',
 3,
 'PUBLISHED',
 '2026-02-10',
 '2026-03-31');

-- Insert mock applications (candidates already applied - ready for interview)
INSERT INTO applications (job_offer_id, interviewee_id, cover_letter, status, applied_date, reviewed_date) VALUES
-- Jane Candidate applied for Java Developer (SHORTLISTED - ready for interview)
(1, 1, 'I am very interested in the Senior Java Developer position and believe my 4 years of experience make me a great fit.',
 'SHORTLISTED', '2026-02-10', '2026-02-12'),

-- John Doe applied for Java Developer (UNDER_REVIEW)
(1, 2, 'With 5 years of Python experience, I am eager to expand my skills to Java development.',
 'UNDER_REVIEW', '2026-02-11', NULL),

-- Jane Smith applied for React Developer (SHORTLISTED)
(2, 3, 'As a passionate React developer with 3 years of experience, I would love to contribute to your team.',
 'SHORTLISTED', '2026-02-12', '2026-02-13'),

-- Mike Chen applied for Java Developer (SHORTLISTED)
(1, 4, 'I have 7 years of Java and microservices experience and am excited about this opportunity.',
 'SHORTLISTED', '2026-02-13', '2026-02-14'),

-- Jane Candidate applied for Python Engineer (SHORTLISTED)
(3, 1, 'I would like to expand my backend skills to Python and believe I can contribute effectively.',
 'SHORTLISTED', '2026-02-13', '2026-02-14'),

-- Mike Chen applied for Python Engineer (UNDER_REVIEW)
(3, 4, 'While my primary expertise is Java, I have strong backend fundamentals that transfer well.',
 'UNDER_REVIEW', '2026-02-14', NULL),

-- Sarah Wilson applied for UX/UI Designer (SHORTLISTED)
(4, 5, 'I am passionate about creating beautiful user experiences and have a strong portfolio to showcase.',
 'SHORTLISTED', '2026-02-14', '2026-02-15'),

-- John Doe applied for React Developer (SHORTLISTED)
(2, 2, 'I have experience with full-stack development and would love to focus more on frontend work.',
 'SHORTLISTED', '2026-02-15', '2026-02-16');

-- ============================================================
-- DATABASE SETUP COMPLETE
-- ============================================================

-- Summary of what was created:
-- ✅ 3 Roles (ADMIN, RECRUITER, INTERVIEWEE)
-- ✅ 7 Test users (1 admin, 1 recruiter, 5 candidates)
-- ✅ All user profiles created
-- ✅ 6 Interview types
-- ✅ 8 Evaluation criteria
-- ✅ 4 Mock job offers (PUBLISHED)
-- ✅ 8 Mock applications (ready for interview scheduling)
-- ✅ 3 Useful views (upcoming_interviews, evaluation_summary, candidate_progress)

-- Test Credentials:
-- Admin:      admin@hirely.com / password123
-- Recruiter:  recruiter@company.com / password123
-- Candidates: jane.candidate@email.com / password123
--            john.doe@email.com / password123
--            jane.smith@email.com / password123
--            mike.chen@email.com / password123
--            sarah.wilson@email.com / password123

SELECT 'Database hirely_db created successfully!' AS Status;

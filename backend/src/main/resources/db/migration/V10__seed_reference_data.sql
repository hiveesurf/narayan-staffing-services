INSERT INTO permissions (code, description) VALUES
    ('DASHBOARD_READ', 'View dashboard'),
    ('JOB_READ', 'View jobs'),
    ('JOB_WRITE', 'Manage jobs'),
    ('APPLICATION_READ', 'View applications'),
    ('APPLICATION_WRITE', 'Manage applications'),
    ('CANDIDATE_READ', 'View candidates'),
    ('CANDIDATE_WRITE', 'Manage candidates'),
    ('INTERVIEW_READ', 'View interviews'),
    ('INTERVIEW_WRITE', 'Manage interviews'),
    ('COMPANY_READ', 'View companies'),
    ('COMPANY_WRITE', 'Manage companies'),
    ('PLACEMENT_READ', 'View placements'),
    ('PLACEMENT_WRITE', 'Manage placements'),
    ('EMPLOYEE_READ', 'View employees'),
    ('EMPLOYEE_WRITE', 'Manage employees'),
    ('PAYROLL_READ', 'View payroll'),
    ('PAYROLL_WRITE', 'Generate and edit payroll'),
    ('PAYROLL_APPROVE', 'Approve payroll'),
    ('PAYROLL_LOCK', 'Lock payroll'),
    ('CMS_READ', 'View website content'),
    ('CMS_WRITE', 'Edit website content'),
    ('USER_MANAGE', 'Manage users and roles'),
    ('AUDIT_READ', 'View audit logs'),
    ('REPORT_READ', 'View and export reports'),
    ('SENSITIVE_READ', 'View sensitive identity and bank fields'),
    ('SETTINGS_MANAGE', 'Manage system settings');

INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Full access'),
    ('ADMIN', 'Operations administrator'),
    ('HR_MANAGER', 'Recruitment and placement'),
    ('PAYROLL_ADMIN', 'Salary and payroll'),
    ('CONTENT_ADMIN', 'Public website content');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_READ', 'JOB_READ', 'JOB_WRITE', 'APPLICATION_READ', 'APPLICATION_WRITE',
    'CANDIDATE_READ', 'CANDIDATE_WRITE', 'INTERVIEW_READ', 'INTERVIEW_WRITE',
    'COMPANY_READ', 'PLACEMENT_READ', 'PLACEMENT_WRITE', 'EMPLOYEE_READ', 'REPORT_READ'
) WHERE r.name = 'HR_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_READ', 'EMPLOYEE_READ', 'EMPLOYEE_WRITE', 'COMPANY_READ',
    'PAYROLL_READ', 'PAYROLL_WRITE', 'PAYROLL_APPROVE', 'PAYROLL_LOCK',
    'REPORT_READ', 'SENSITIVE_READ'
) WHERE r.name = 'PAYROLL_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'DASHBOARD_READ', 'CMS_READ', 'CMS_WRITE'
) WHERE r.name = 'CONTENT_ADMIN';

INSERT INTO salary_component_definitions (name, code, component_type, calculation_type, default_value, display_order) VALUES
    ('Basic Salary', 'BASIC', 'EARNING', 'FIXED', 0, 10),
    ('HRA', 'HRA', 'EARNING', 'PERCENT_OF_BASIC', 40, 20),
    ('Conveyance', 'CONVEYANCE', 'EARNING', 'FIXED', 1600, 30),
    ('Special Allowance', 'SPECIAL_ALLOWANCE', 'EARNING', 'FIXED', 0, 40),
    ('Medical Allowance', 'MEDICAL', 'EARNING', 'FIXED', 1250, 50),
    ('Bonus', 'BONUS', 'EARNING', 'FIXED', 0, 60),
    ('Overtime', 'OVERTIME', 'EARNING', 'FIXED', 0, 70),
    ('Other Earnings', 'OTHER_EARNING', 'EARNING', 'FIXED', 0, 80),
    ('PF', 'PF', 'DEDUCTION', 'PERCENT_OF_BASIC', 12, 110),
    ('ESI', 'ESI', 'DEDUCTION', 'PERCENT_OF_GROSS', 0.75, 120),
    ('Professional Tax', 'PT', 'DEDUCTION', 'FIXED', 200, 130),
    ('TDS', 'TDS', 'DEDUCTION', 'FIXED', 0, 140),
    ('Loan Deduction', 'LOAN', 'DEDUCTION', 'FIXED', 0, 150),
    ('Advance Deduction', 'ADVANCE', 'DEDUCTION', 'FIXED', 0, 160),
    ('Other Deduction', 'OTHER_DEDUCTION', 'DEDUCTION', 'FIXED', 0, 170);

INSERT INTO document_requirements (code, label, description, required, display_order) VALUES
    ('RESUME', 'Resume', 'PDF or Word resume', TRUE, 1),
    ('PHOTOGRAPH', 'Photograph', 'Recent passport-size photograph', FALSE, 2),
    ('ID_PROOF', 'Identity proof', 'Optional. Aadhaar or PAN is not required to apply.', FALSE, 3);

INSERT INTO job_categories (name, slug, description, icon, display_order) VALUES
    ('Engineering', 'engineering', 'Design, production and plant engineering roles', 'cog', 1),
    ('IT', 'it', 'Software, infrastructure and support roles', 'monitor', 2),
    ('Manufacturing', 'manufacturing', 'Shop-floor, quality and production roles', 'factory', 3),
    ('Sales', 'sales', 'Business development and account roles', 'handshake', 4),
    ('Administration', 'administration', 'Office administration and coordination', 'clipboard', 5),
    ('HR', 'hr', 'Human resources and employee relations', 'users', 6),
    ('Finance', 'finance', 'Accounts, payroll support and finance', 'landmark', 7),
    ('Operations', 'operations', 'Plant, warehouse and site operations', 'workflow', 8),
    ('Security', 'security', 'Industrial and facility security', 'shield', 9),
    ('Facility Management', 'facility-management', 'Soft services and site upkeep', 'building', 10),
    ('Skilled Worker', 'skilled-worker', 'Trades and certified technicians', 'wrench', 11),
    ('Unskilled Worker', 'unskilled-worker', 'General workforce and helpers', 'hard-hat', 12);

INSERT INTO industries (name, slug, description, icon, display_order) VALUES
    ('Manufacturing', 'manufacturing', 'Discrete and process manufacturing plants', 'factory', 1),
    ('Engineering', 'engineering', 'Project and industrial engineering', 'drafting-compass', 2),
    ('Construction', 'construction', 'Sites, contractors and infrastructure', 'hard-hat', 3),
    ('Automotive', 'automotive', 'OEMs, ancillaries and auto clusters', 'car', 4),
    ('Logistics', 'logistics', 'Warehousing, transport and dispatch', 'truck', 5),
    ('IT', 'it', 'Technology products and services', 'monitor', 6),
    ('Hospitality', 'hospitality', 'Hotels, catering and guest services', 'utensils', 7),
    ('Facility Management', 'facility-management', 'Integrated facility contracts', 'building', 8);

INSERT INTO services (title, slug, short_description, detailed_description, icon, display_order, seo_title, seo_description) VALUES
    ('Facility Management', 'facility-management',
     'Deployed teams for housekeeping, maintenance and site support.',
     $$Narayan Staffing Services supplies and supervises facility teams for industrial and commercial sites. Engagements cover housekeeping, technical helpers, gardeners, and multi-shift site support, with attendance discipline and a single point of contact for the client.$$,
     'building', 1, 'Facility Management Staffing | Narayan Staffing Services',
     'Facility management manpower for industrial and commercial sites across India.'),
    ('Manpower Recruitment', 'manpower-recruitment',
     'Permanent and contract hiring for plants, projects and offices.',
     $$We source, screen and deploy people for manufacturing, engineering, logistics and corporate roles. Each assignment follows a documented pipeline from job intake to joining, so clients can see who was screened, interviewed and selected.$$,
     'users', 2, 'Manpower Recruitment | Narayan Staffing Services',
     'Contract and permanent recruitment for plants, projects and offices.'),
    ('Third Party Inspection', 'third-party-inspection',
     'Independent inspection support for quality-sensitive supply chains.',
     $$Our inspection coordinators support incoming, in-process and dispatch checks for clients who need an independent pair of eyes on the shop floor. Findings are recorded against the job and shared with the client team.$$,
     'search-check', 3, 'Third Party Inspection | Narayan Staffing Services',
     'Independent inspection manpower for manufacturing and vendor sites.'),
    ('White Collar Staffing', 'white-collar-staffing',
     'Professionals for finance, HR, engineering and operations desks.',
     $$We recruit supervisors, engineers, coordinators and office professionals who can join client teams with a clear role, salary and reporting line. Offers and joining are tracked as placements, not overwritten profiles.$$,
     'briefcase', 4, 'White Collar Staffing | Narayan Staffing Services',
     'Professional staffing for engineering, finance, HR and operations.'),
    ('Skill Development', 'skill-development',
     'Role-ready induction for deployed workforces.',
     $$Before a batch is deployed, we align people to the role: safety briefing, trade orientation and site rules. The aim is a workforce that can start work without a long client-side ramp-up.$$,
     'graduation-cap', 5, 'Skill Development | Narayan Staffing Services',
     'Induction and trade orientation for deployed industrial workforces.'),
    ('Payroll Management', 'payroll-management',
     'Monthly salary structures, payroll runs and salary slips.',
     $$For workforces we manage, salary components, payable days and deductions are configured per employee. Monthly payroll can be drafted, approved and locked, and each employee receives a salary slip.$$,
     'wallet', 6, 'Payroll Management | Narayan Staffing Services',
     'Configurable salary structures, monthly payroll and salary slips.');

INSERT INTO site_settings (setting_key, setting_value) VALUES
    ('address', '24, 3rd floor, Shriram Sankul, Chakan, Talegaon Road, Near Mayur Hotel, Pune, Maharashtra 410501'),
    ('phone', '+91 7903271752'),
    ('email', 'hr@narayanstaffingservices.com'),
    ('hours', 'Monday to Saturday, 9:30 am – 6:30 pm'),
    ('maps_url', 'https://www.google.com/maps/search/?api=1&query=Shriram+Sankul+Chakan+Pune'),
    ('facebook', ''),
    ('linkedin', ''),
    ('instagram', ''),
    ('whatsapp', '917903271752'),
    ('logo_text', 'Narayan'),
    ('legal_name', 'Narayan Staffing Services');

INSERT INTO website_sections (page_key, section_key, title, subtitle, content, cta_label, cta_url, secondary_cta_label, secondary_cta_url, display_order) VALUES
    ('HOME', 'HERO', 'Trusted Manpower & Staffing Solutions',
     'Recruitment, staffing, payroll, facility management and third-party inspection for plants and offices across India. Based in Chakan, Pune, working PAN India.',
     NULL, 'View Jobs', '/careers', 'Contact Us', '/contact', 1),
    ('HOME', 'ABOUT_SUMMARY', 'A workforce partner, not a job board',
     'From the first application to the monthly salary slip.',
     $$Narayan Staffing Services recruits and manages people for manufacturing, engineering, logistics and facilities. Clients get a named team, a documented selection process, and payroll that can be audited month by month.$$,
     'About the company', '/about', NULL, NULL, 2),
    ('HOME', 'CTA', 'Looking for the Right Workforce?',
     'Tell us the role, the site and the shift. We will shortlist people who can actually join.',
     NULL, 'Hire Talent', '/contact', 'Browse Jobs', '/careers', 3),
    ('ABOUT', 'INTRO', 'About Narayan Staffing Services',
     'Manpower, inspection and payroll from Pune.',
     $$Narayan Staffing Services is an HR, labour contract and third-party inspection practice operating from Chakan, Pune, with deployments across India. We introduce ourselves as a manpower supply firm for plants, projects and offices that need people who will stay, not just a pile of resumes.

The work is practical. We take a requirement, publish the role, screen applicants, interview the shortlist, and place selected people on the client site. Where we also run payroll, the salary structure, the monthly run and the salary slip stay attached to that same person.$$,
     NULL, NULL, NULL, NULL, 1),
    ('ABOUT', 'MISSION', 'Mission', NULL,
     $$To put reliable people into real jobs, and to keep the record of how they were hired, where they are placed, and what they are paid.$$,
     NULL, NULL, NULL, NULL, 2),
    ('ABOUT', 'VISION', 'Vision', NULL,
     $$To be the staffing desk manufacturing and facility clients in western India call when the shift cannot run short.$$,
     NULL, NULL, NULL, NULL, 3),
    ('CONTACT', 'INTRO', 'Talk to the staffing desk',
     'Hiring a crew, listing a vacancy, or asking about payroll.',
     $$Write to us with the site location, the role and the number of people. We reply during working hours.$$,
     NULL, NULL, NULL, NULL, 1),
    ('FOOTER', 'BLURB', 'Narayan Staffing Services', NULL,
     $$Manpower recruitment, facility staffing, payroll and third-party inspection from Chakan, Pune.$$,
     NULL, NULL, NULL, NULL, 1),
    ('PRIVACY', 'BODY', 'Privacy Policy', NULL,
     $$Narayan Staffing Services collects the details you submit on a job application or the contact form so we can consider you for a role or reply to an enquiry.

Application data includes your name, contact details, address, education, experience and the documents you choose to upload. Identity numbers and bank details, when later recorded for employment, are restricted to payroll and authorised administrators and are masked in ordinary screens.

We do not sell candidate data. Access inside the company is limited by role. You may write to hr@narayanstaffingservices.com to ask what we hold about you or to request a correction.

This notice may be updated from the website settings. The version on this page is the current one.$$,
     NULL, NULL, NULL, NULL, 1),
    ('TERMS', 'BODY', 'Terms of Use', NULL,
     $$This website publishes vacancies and company information for Narayan Staffing Services. Applying for a job is an expression of interest. It is not an offer of employment.

Information you submit must be accurate. We may reject an application, close a vacancy, or withdraw a process without notice when a role is filled or a client changes the requirement.

Job descriptions are written for the assignment as given by the client and may change. Salary figures, when shown, are ranges and not a commitment until a placement is confirmed.

These terms cover use of the public website. Employment, where offered, is governed by the appointment documents issued at joining.$$,
     NULL, NULL, NULL, NULL, 1);

INSERT INTO content_blocks (group_key, title, subtitle, body, icon, metric_value, link_url, display_order) VALUES
    ('NAV', 'Home', NULL, NULL, NULL, NULL, '/', 1),
    ('NAV', 'About', NULL, NULL, NULL, NULL, '/about', 2),
    ('NAV', 'Services', NULL, NULL, NULL, NULL, '/services', 3),
    ('NAV', 'Careers', NULL, NULL, NULL, NULL, '/careers', 4),
    ('NAV', 'Clients', NULL, NULL, NULL, NULL, '/clients', 5),
    ('NAV', 'Gallery', NULL, NULL, NULL, NULL, '/gallery', 6),
    ('NAV', 'Contact', NULL, NULL, NULL, NULL, '/contact', 7),
    ('STATS', 'Candidates Placed', NULL, NULL, NULL, '2,400+', NULL, 1),
    ('STATS', 'Active Clients', NULL, NULL, NULL, '80+', NULL, 2),
    ('STATS', 'Open Positions', NULL, NULL, NULL, '35+', NULL, 3),
    ('STATS', 'Cities Served', NULL, NULL, NULL, '18', NULL, 4),
    ('WHY_CHOOSE_US', 'Skilled Workforce', NULL, 'People are screened for the trade and the shift, not only for a keyword on a resume.', 'badge-check', NULL, NULL, 1),
    ('WHY_CHOOSE_US', 'Fast Recruitment', NULL, 'Shortlists move on a visible pipeline, from application to interview to joining.', 'timer', NULL, NULL, 2),
    ('WHY_CHOOSE_US', 'Industry Expertise', NULL, 'Most of our work sits in manufacturing, automotive, engineering and facilities.', 'factory', NULL, NULL, 3),
    ('WHY_CHOOSE_US', 'Reliable Staffing', NULL, 'Deployments are tracked as placements, so a replacement does not erase the history.', 'shield', NULL, NULL, 4),
    ('WHY_CHOOSE_US', 'Transparent Process', NULL, 'Status changes, interview notes and payroll locks are recorded.', 'scroll-text', NULL, NULL, 5),
    ('WHY_CHOOSE_US', 'PAN India Support', NULL, 'The desk is in Chakan, Pune. Assignments run across plants and cities in India.', 'map', NULL, NULL, 6),
    ('VALUES', 'Straight dealing', NULL, 'Say what the wage, the shift and the site actually are.', NULL, NULL, NULL, 1),
    ('VALUES', 'Care with records', NULL, 'A person''s applications, placements and pay stay connected.', NULL, NULL, NULL, 2),
    ('VALUES', 'Respect on site', NULL, 'Workers are colleagues of the client, not a headcount on a sheet.', NULL, NULL, NULL, 3),
    ('VALUES', 'Accountability', NULL, 'Someone on our desk owns the requirement until people have joined.', NULL, NULL, NULL, 4),
    ('PROCESS', 'Understand the role', '01', 'Site, shift, skill and the number of people. We write the job the way a candidate will read it.', NULL, NULL, NULL, 1),
    ('PROCESS', 'Source and screen', '02', 'Applications are reviewed against experience, location and pay expectation.', NULL, NULL, NULL, 2),
    ('PROCESS', 'Interview', '03', 'Shortlisted people meet the interviewer in person, on a call, or by video.', NULL, NULL, NULL, 3),
    ('PROCESS', 'Select and place', '04', 'A selected candidate becomes an employee profile and a placement with the client.', NULL, NULL, NULL, 4),
    ('PROCESS', 'Pay and retain', '05', 'Where we run payroll, the salary structure follows the person month by month.', NULL, NULL, NULL, 5),
    ('TEAM', 'Recruitment Desk', 'Chakan, Pune', 'Owns job intake, screening and interviews.', NULL, NULL, NULL, 1),
    ('TEAM', 'Client Staffing', 'Deployments', 'Matches selected people to sites and keeps placement history.', NULL, NULL, NULL, 2),
    ('TEAM', 'Payroll Desk', 'Salary & slips', 'Maintains salary structures, monthly runs and salary slips.', NULL, NULL, NULL, 3),
    ('TRUST', 'Documented selection', NULL, 'Every stage from applied to joined is a status, not a verbal update.', NULL, NULL, NULL, 1),
    ('TRUST', 'One person, many assignments', NULL, 'Someone can be rejected for one job and selected for another without losing history.', NULL, NULL, NULL, 2),
    ('TRUST', 'Payroll that adds up', NULL, 'Earnings and deductions are configured. Net pay is calculated, not typed in.', NULL, NULL, NULL, 3),
    ('TRUST', 'A desk you can visit', NULL, 'Shriram Sankul, Chakan. Call or write during working hours.', NULL, NULL, NULL, 4);

INSERT INTO testimonials (author_name, author_role, company_name, quote, display_order) VALUES
    ('Plant HR', 'Human Resources', 'Automotive ancillary, Chakan',
     'They filled a technician batch before the line ramp and kept the names, the joining dates and the exits in one place.', 1),
    ('Admin Head', 'Administration', 'Industrial campus, Pune',
     'Facility staff arrived with a supervisor we could call. Replacements were tracked instead of being handwritten over the old list.', 2),
    ('Finance Controller', 'Finance', 'Engineering works, Pimpri',
     'Monthly payroll arrived with a slip per person. When a salary changed, the earlier months stayed as they were.', 3);

INSERT INTO website_clients (name, display_order) VALUES
    ('Deccan Auto Components', 1),
    ('Western Warehousing', 2),
    ('Shakti Facilities', 3),
    ('Sahyadri Engineering', 4),
    ('BluePeak Systems', 5),
    ('Khandala Hospitality', 6);

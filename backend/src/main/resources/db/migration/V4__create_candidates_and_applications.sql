CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(30),
    mobile VARCHAR(20) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    alternate_phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(12),
    country VARCHAR(80) DEFAULT 'India',
    permanent_address TEXT,
    father_name VARCHAR(150),
    mother_name VARCHAR(150),
    marital_status VARCHAR(30),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(20),
    highest_qualification VARCHAR(150),
    specialization VARCHAR(150),
    total_experience NUMERIC(5, 2),
    relevant_experience NUMERIC(5, 2),
    current_company VARCHAR(180),
    current_job_title VARCHAR(150),
    current_salary NUMERIC(14, 2),
    expected_salary NUMERIC(14, 2),
    notice_period VARCHAR(60),
    photo_path VARCHAR(500),
    aadhaar_encrypted TEXT,
    pan_encrypted TEXT,
    other_id VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_candidates_name ON candidates (full_name);
CREATE INDEX idx_candidates_city ON candidates (city);
CREATE INDEX idx_candidates_mobile ON candidates (mobile);

CREATE TABLE candidate_education (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
    degree VARCHAR(150) NOT NULL,
    specialization VARCHAR(150),
    institution VARCHAR(200),
    passing_year INT,
    score VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_candidate_education_candidate ON candidate_education (candidate_id);

CREATE TABLE candidate_experience (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
    company VARCHAR(180) NOT NULL,
    designation VARCHAR(150),
    joining_date DATE,
    leaving_date DATE,
    salary NUMERIC(14, 2),
    responsibilities TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_candidate_experience_candidate ON candidate_experience (candidate_id);

CREATE TABLE candidate_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates (id) ON DELETE CASCADE,
    skill_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_id, skill_name)
);

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_number VARCHAR(40) NOT NULL UNIQUE,
    candidate_id UUID NOT NULL REFERENCES candidates (id),
    job_id UUID NOT NULL REFERENCES jobs (id),
    status VARCHAR(40) NOT NULL DEFAULT 'APPLIED',
    consent BOOLEAN NOT NULL DEFAULT FALSE,
    cover_note TEXT,
    assigned_hr_id UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_id, job_id)
);

CREATE INDEX idx_applications_status ON applications (status);
CREATE INDEX idx_applications_job ON applications (job_id);
CREATE INDEX idx_applications_candidate ON applications (candidate_id);
CREATE INDEX idx_applications_created ON applications (created_at);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    old_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    changed_by UUID REFERENCES users (id),
    reason VARCHAR(500),
    note TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_status_history_app ON application_status_history (application_id, changed_at);

CREATE TABLE application_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    author_id UUID REFERENCES users (id),
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_notes_app ON application_notes (application_id, created_at);

CREATE SEQUENCE application_no_seq START 1;

CREATE TABLE job_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(60),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    job_code VARCHAR(40) NOT NULL UNIQUE,
    category_id UUID REFERENCES job_categories (id),
    company_id UUID NOT NULL REFERENCES companies (id),
    location VARCHAR(150) NOT NULL,
    department VARCHAR(120),
    employment_type VARCHAR(40) NOT NULL,
    experience_min NUMERIC(4, 1),
    experience_max NUMERIC(4, 1),
    education VARCHAR(180),
    salary_min NUMERIC(14, 2),
    salary_max NUMERIC(14, 2),
    salary_type VARCHAR(20),
    vacancies INT NOT NULL DEFAULT 1,
    job_description TEXT,
    responsibilities TEXT,
    required_skills TEXT,
    preferred_skills TEXT,
    benefits TEXT,
    deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    publish_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_category ON jobs (category_id);
CREATE INDEX idx_jobs_company ON jobs (company_id);
CREATE INDEX idx_jobs_location ON jobs (location);
CREATE INDEX idx_jobs_featured ON jobs (featured);

CREATE SEQUENCE job_code_seq START 1;
CREATE SEQUENCE company_code_seq START 1;

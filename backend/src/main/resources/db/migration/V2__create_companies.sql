CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_code VARCHAR(40) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    industry VARCHAR(120),
    company_type VARCHAR(80),
    website VARCHAR(255),
    email VARCHAR(180),
    phone VARCHAR(30),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(12),
    gst_number VARCHAR(20),
    pan VARCHAR(20),
    contact_person VARCHAR(150),
    contact_person_designation VARCHAR(120),
    contact_person_phone VARCHAR(30),
    contact_person_email VARCHAR(180),
    billing_address TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PROSPECT',
    notes TEXT,
    logo_path VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_companies_status ON companies (status);
CREATE INDEX idx_companies_name ON companies (company_name);
CREATE INDEX idx_companies_city ON companies (city);
